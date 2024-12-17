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

static struct k_work_delayable notify_work;

#define RUN_STATUS_LED          DK_LED1
#define CON_STATUS_LED          DK_LED2
#define SAMPLE_INTERVAL  4000
#define NOTIFY_INTERVAL 100

extern const eeg_sample_t eeg_data_array[];
extern const size_t eeg_data_size;
float this_sample[18];

/* EEG Service UUID */
static struct bt_uuid_128 eeg_service_uuid = BT_UUID_INIT_128(
    0xCA, 0x34, 0x9C, 0x5F, 0x60, 0x00, 0x00, 0x88,
    0x00, 0x11, 0x00, 0x00, 0x5D, 0x74, 0x00, 0x00);

/* EEG Characteristic UUID */
static struct bt_uuid_128 eeg_char_uuid = BT_UUID_INIT_128(
    0xFB, 0x21, 0x4A, 0x5A, 0x80, 0x00, 0x00, 0x89,
    0x00, 0x43, 0x00, 0x00, 0x27, 0x2A, 0x00, 0x00);

static struct bt_conn *current_conn;
static size_t sample_index = 0;
static size_t channel_index = 0;
static struct k_timer notification_timer;

static const uint8_t ad_flags[] = { BT_LE_AD_GENERAL | BT_LE_AD_NO_BREDR };

static const char name[] = "EEG Sensor";

static bool notifications_enabled =  false; // Track notification state

void list_devices(void)
{
    printk("Listing all devices with status 'okay'...\n");

    /* Check all nodes with compatible property set to "zephyr,cdc-acm-uart" */
    DT_FOREACH_STATUS_OKAY(zephyr_cdc_acm_uart, PRINT_NODE);
}

static ssize_t read_eeg(struct bt_conn *conn, const struct bt_gatt_attr *attr,
                        void *buf, uint16_t len, uint16_t offset)
{
    const uint8_t *value = attr->user_data;
    return bt_gatt_attr_read(conn, attr, buf, len, offset, value,
                             eeg_data_size);
}
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
static struct bt_gatt_attr eeg_attrs[] = {
    BT_GATT_PRIMARY_SERVICE(&eeg_service_uuid),
    BT_GATT_CHARACTERISTIC(&eeg_char_uuid.uuid, BT_GATT_CHRC_NOTIFY,
                        BT_GATT_PERM_READ, read_eeg, NULL, (void *)&eeg_data_array[0]),
    BT_GATT_CCC(ccc_cfg_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),
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
    if (channel_index == 0) { // if we are at the start of a new sample
        for(int i = 0; i < 18; i++) {
            this_sample[i] = eeg_data_array[sample_index].channels[i];
        }
    }
    int err = bt_gatt_notify(current_conn, &eeg_attrs[1], &this_sample[channel_index], sizeof(float));
    if (err) {
        printk("Failed to notify, err %d\n", err);
    } else {
        printk("Notification sent: %d\n", this_sample[channel_index]);
        channel_index++;
        if(channel_index >= 18){
            channel_index = 0;
            sample_index++;
            if(sample_index == 8192){
                sample_index = 0;
            }
        }
    }
    // Reschedule next notification
    if (channel_index == 0) {
        k_work_reschedule(&notify_work, K_MSEC(SAMPLE_INTERVAL)); // Wait for the next sample
    } else {
        k_work_reschedule(&notify_work, K_MSEC(NOTIFY_INTERVAL)); // Send the next channel
    }
}

static void notification_timer_handler(struct k_timer *timer)
{
    // Instead of notifying here, schedule the work
    k_work_submit(&notify_work.work);
}

static struct bt_gatt_service eeg_service = BT_GATT_SERVICE(eeg_attrs);

static void connected(struct bt_conn *conn, uint8_t err)
{
    if (err) {
        printk("Connection failed, err 0x%02x %s\n", err, bt_hci_err_to_str(err));
        return;
    }
    printk("Connected\n");
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
    k_timer_stop(&notification_timer); // Stop notifications on disconnect
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
    k_timer_init(&notification_timer, notification_timer_handler, NULL);

    printk("Starting Bluetooth EEG Sensor simulation\n");

    err = bt_enable(NULL);
    if (err) {
        printk("Bluetooth init failed (err %d)\n", err);
        return 1;
    }
    printk("Bluetooth initialized!\n");
    
    err = settings_load();// Load Bluetooth settings
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

    channel_index = 0;
    sample_index = 0;

    for (;;) {
        k_sleep(K_MSEC(NOTIFY_INTERVAL / 2));
        k_sleep(K_MSEC(NOTIFY_INTERVAL / 2));
    }
}