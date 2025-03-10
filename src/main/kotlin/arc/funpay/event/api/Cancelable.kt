package arc.funpay.event.api

/**
 * Interface representing a cancelable event.
 */
interface Cancelable {
    /**
     * Indicates whether the event is cancelled.
     */
    var isCancelled: Boolean
}