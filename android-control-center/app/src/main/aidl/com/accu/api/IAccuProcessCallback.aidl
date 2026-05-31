// IAccuProcessCallback.aidl
package com.accu.api;

/**
 * Streaming callback for long-running shell processes.
 * Delivered on a background thread — post to your own handler/coroutine.
 */
oneway interface IAccuProcessCallback {
    void onStdoutLine(String line);
    void onStderrLine(String line);
    void onExit(int exitCode);
}
