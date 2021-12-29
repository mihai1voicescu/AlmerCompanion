package io.almer.companionshared.server
import java.util.*

/**
 * Constants for use in the Bluetooth LE Chat sample
 */
/**
 * UUID identified with this app - set as Service UUID for BLE Chat.
 *
 * Bluetooth requires a certain format for UUIDs associated with Services.
 * The official specification can be found here:
 * [://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery][https]
 */
//val SERVICE_UUID: UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
val SERVICE_UUID: UUID = UUID.fromString("ff578b5f-68ba-4843-9a89-50f1822b27dc")

/**
 * UUID for the message
 */
val MESSAGE_UUID: UUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b")

/**
 * UUID to confirm device connection
 */
val CONFIRM_UUID: UUID = UUID.fromString("36d4dc5c-814b-4097-a5a6-b93b39085928")

const val REQUEST_ENABLE_BT = 1
