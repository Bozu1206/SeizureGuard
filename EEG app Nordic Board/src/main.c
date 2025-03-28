#include <zephyr/types.h>
#include <stddef.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include <zephyr/sys/printk.h>
#include <zephyr/sys/byteorder.h>
#include <zephyr/kernel.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/bluetooth/hci.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/bluetooth/gatt.h>
#include <zephyr/settings/settings.h>
#include <zephyr/device.h>
#include "eeg_data.h"

// Define work for notifications
static struct k_work_delayable notify_work;

// Define constants
#define RUN_STATUS_LED          DK_LED1
#define CON_STATUS_LED          DK_LED2
#define NOTIFY_INTERVAL_NORMAL         150    // Interval for sending data packets (ms)
#define NOTIFY_INTERVAL_LOW_POWER      1000    // Interval for sending data packets (ms)
#define NOTIFY_INTERVAL_PERFORMANCE    200    // Interval for sending data packets (ms)
#define BATCH_INTERVAL          4000   // Wait time between batches (ms)
#define SAMPLE_BATCH_SIZE       1024   // Number of samples per batch
#define FLOATS_PER_SAMPLE       18     // Number of floats per sample
#define FLOATS_PER_NOTIFICATION 18*4     // Number of floats per notification

extern const eeg_sample_t eeg_data_array[];
extern const size_t eeg_data_size;

static size_t notify_interval;

// Buffer to hold multiple floats
static float notification_buffer[FLOATS_PER_NOTIFICATION];
static size_t floats_in_buffer = 0;

// Define GATT UUIDs
static struct bt_uuid_128 eeg_service_uuid = BT_UUID_INIT_128(
    0xCA, 0x34, 0x9C, 0x5F, 0x60, 0x00, 0x00, 0x88,
    0x00, 0x11, 0x00, 0x00, 0x5D, 0x74, 0x00, 0x00);

// Data characteristic UUID
static struct bt_uuid_128 eeg_char_uuid = BT_UUID_INIT_128(
    0xFB, 0x21, 0x4A, 0x5A, 0x80, 0x00, 0x00, 0x89,
    0x00, 0x43, 0x00, 0x00, 0x27, 0x2A, 0x00, 0x00);

// Configuration characteristic UUID
static struct bt_uuid_128 config_char_uuid = BT_UUID_INIT_128(
    0x46, 0x1A, 0xB7, 0xDF, 0x2C, 0x98, 0x11, 0xEB,
    0xAD, 0xC6, 0xF8, 0xE5, 0x8E, 0x96, 0x54, 0xBB);

static struct bt_conn *current_conn = NULL;
static size_t sample_index = 0;
static size_t channel_index = 0;
static size_t samples_sent_in_batch = 0;   // Samples sent in the current batch

static const uint8_t ad_flags[] = { BT_LE_AD_GENERAL | BT_LE_AD_NO_BREDR };
static const char name[] = "EEG Sensor";
static bool notifications_enabled = false; // Track notification state

static uint8_t config_value = 0;

static void ccc_cfg_changed(const struct bt_gatt_attr *attr, uint16_t value)
{
    if (value == BT_GATT_CCC_NOTIFY) {
        notifications_enabled = true;
        printk("Notifications enabled\n");
        k_work_submit(&notify_work.work); // Start the notification work
    } else {
        notifications_enabled = false;
        printk("Notifications disabled\n");
        k_work_cancel_delayable(&notify_work);
    }
}

/* Callback function that gets triggered when an ATT MTU exchange is completed */
static void att_mtu_updated(struct bt_conn *conn, uint16_t tx, uint16_t rx)
{
    /* The final, effective MTU for this connection */
    uint16_t mtu = bt_gatt_get_mtu(conn);

    printk("MTU updated: TX %u, RX %u, Effective %u\n", tx, rx, mtu);
}
static struct bt_gatt_cb gatt_callbacks = {
    .att_mtu_updated = att_mtu_updated,
};

static ssize_t write_config_value(struct bt_conn *conn,
                                  const struct bt_gatt_attr *attr,
                                  const void *buf,
                                  uint16_t len,
                                  uint16_t offset,
                                  uint8_t flags)
{
    if (len != 1) {
        printk("Config characteristic expects exactly 1 byte\n");
        return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
    }

    memcpy(&config_value, buf, 1);   // Copy the single byte from client into config_value
    switch (config_value)
    {
    case 1: 
        notify_interval = NOTIFY_INTERVAL_LOW_POWER;
        break;
    case 2:
        notify_interval = NOTIFY_INTERVAL_PERFORMANCE;
        break;
    default:
        notify_interval = NOTIFY_INTERVAL_NORMAL;
        break;
    }
    return len;  // Return number of bytes written
}

// Define GATT attributes without read callback
static struct bt_gatt_attr eeg_attrs[] = {
    BT_GATT_PRIMARY_SERVICE(&eeg_service_uuid),
    BT_GATT_CHARACTERISTIC(&eeg_char_uuid.uuid, BT_GATT_CHRC_NOTIFY, // Data characteristic
                           0, NULL, NULL, NULL), 
    BT_GATT_CCC(ccc_cfg_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),
    BT_GATT_CHARACTERISTIC(&config_char_uuid.uuid,   // Configuration characteristic
                        BT_GATT_CHRC_WRITE | BT_GATT_CHRC_WRITE_WITHOUT_RESP,
                        BT_GATT_PERM_WRITE,
                        NULL,
                        write_config_value,
                        &config_value),
};

void check_device(const char *device_name)
{
    const struct device *dev = device_get_binding(device_name);
    if (dev) {
        printk("Device found: %s\n", device_name);
    } else {
        printk("Device not found: %s\n", device_name);
    }
}

// Notification work handler
static void notify_work_handler(struct k_work *work)
{
    if (!current_conn) {
        printk("No valid connection\n");
        return;
    }
    if (!notifications_enabled) {
        printk("Device is not subscribed to characteristic\n");
        return;
    }
    // Check if we've sent SAMPLE_BATCH_SIZE samples in the current batch
    if (samples_sent_in_batch >= SAMPLE_BATCH_SIZE) {
        printk("Batch complete.\n");
        // Reset the batch counter
        samples_sent_in_batch = 0;
        // Reschedule the next batch after BATCH_INTERVAL
        k_work_reschedule(&notify_work, K_MSEC(notify_interval));
        return;
    }
    // Fill the notification buffer with FLOATS_PER_NOTIFICATION floats
    floats_in_buffer = 0;
    while (floats_in_buffer < (size_t)FLOATS_PER_NOTIFICATION && samples_sent_in_batch < SAMPLE_BATCH_SIZE) {
        if (sample_index >= 8192) {
            sample_index = 0; // Wrap around if end is reached
        }
        // Add the current float to the buffer
        notification_buffer[floats_in_buffer] = eeg_data_array[sample_index].channels[channel_index];
        floats_in_buffer++;
        // Update channel and sample indices
        channel_index++;
        if (channel_index >= FLOATS_PER_SAMPLE) {
            channel_index = 0;
            sample_index++;
            samples_sent_in_batch++;
        }
    }
    // Send the buffer as a notification if it has any floats
    if (floats_in_buffer > 0) {
        int err = bt_gatt_notify(current_conn, &eeg_attrs[1], notification_buffer, floats_in_buffer * sizeof(float));
        if (err) {
            samples_sent_in_batch --;
            sample_index --;
            printk("Failed to notify, err %d; samples_sent_in_batch: %d\n", err, samples_sent_in_batch);
        } else {
            // printk("Notification sent: %d floats; samples_sent_in_batch: %d\n", floats_in_buffer, samples_sent_in_batch);
        }
    }
    k_work_reschedule(&notify_work, K_MSEC(notify_interval));
}

// Register GATT service
static struct bt_gatt_service eeg_service = BT_GATT_SERVICE(eeg_attrs);

// Connection callbacks
static void connected(struct bt_conn *conn, uint8_t err)
{
    if (err) {
        printk("Connection failed, err 0x%02x %s\n", err, bt_hci_err_to_str(err));
        return;
    }
    printk("Connected\n");
    printk("Current mtu: %d\n", bt_gatt_get_mtu(conn));
    current_conn = bt_conn_ref(conn);
}

static void disconnected(struct bt_conn *conn, uint8_t reason)
{
    printk("Disconnected, reason 0x%02x\n", reason);
    if (current_conn) {
        bt_conn_unref(current_conn);
        current_conn = NULL;
    }
    notifications_enabled = false;
    k_work_cancel_delayable(&notify_work);
}

BT_CONN_CB_DEFINE(conn_callbacks) = {
    .connected = connected,
    .disconnected = disconnected,
};

int main(void)
{
    int err;

    k_work_init_delayable(&notify_work, notify_work_handler);

    printk("Starting Bluetooth EEG Sensor simulation!\n");

    err = bt_enable(NULL);
    if (err) {
        printk("Bluetooth init failed (err %d)\n", err);
        return 1;
    }
    printk("Bluetooth initialized!\n");

    bt_gatt_cb_register(&gatt_callbacks); // enable GATT callbacks
    
    err = settings_load(); // Load Bluetooth settings
    if (err) {
        printk("Failed to load settings (err %d)\n", err);
        return 1;
    }
    printk("Settings loaded\n");

    err = bt_gatt_service_register(&eeg_service);
    if (err) {
        printk("Failed to register EEG service (err %d)\n", err);
        return 1;
    }
    printk("EEG service registered\n");

    static const struct bt_data ad[] = { // advertising data
        BT_DATA(BT_DATA_FLAGS, ad_flags, sizeof(ad_flags)),
        BT_DATA(BT_DATA_NAME_COMPLETE, name, (sizeof(name) - 1)),
       // BT_DATA(BT_DATA_UUID128_ALL, ad_uuid, sizeof(ad_uuid)),
    };

    err = bt_le_adv_start(BT_LE_ADV_CONN, ad, ARRAY_SIZE(ad), NULL, 0);
    if (err) {
        printk("Advertising failed to start (err %d)\n", err);
        return 1;
    }

    printk("Advertising successfully started\n");

    notify_interval = NOTIFY_INTERVAL_NORMAL;
    channel_index = 0;
    sample_index = 0;

    for (;;) {
        k_sleep(K_FOREVER);
    }
}
