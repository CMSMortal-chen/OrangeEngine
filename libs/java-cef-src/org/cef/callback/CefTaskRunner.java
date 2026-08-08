// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.callback;

/**
 * Post CefTask instances to specific CEF threads. Required to marshal
 * browser calls to the CEF UI thread when
 * {@code CefSettings.multi_threaded_message_loop} is enabled.
 */
public class CefTaskRunner {
    public static final int TID_UI = 0;
    public static final int TID_IO = 1;
    public static final int TID_FILE_BACKGROUND = 2;
    public static final int TID_FILE_USER_VISIBLE = 3;
    public static final int TID_FILE_USER_BLOCKING = 4;
    public static final int TID_PROCESS_LAUNCHER = 5;
    public static final int TID_RENDERER = 6;

    /**
     * Post a task for execution on the specified thread.
     *
     * @param task The task to execute.
     * @param threadId The id of the target thread (one of the TID_* constants).
     * @return true if the task was posted successfully, false otherwise.
     */
    public static boolean postTask(CefTask task, int threadId) {
        return N_PostTask(threadId, task);
    }

    /**
     * Returns true if the calling thread is the CEF UI thread. Used to avoid
     * posting a task that waits on itself (deadlock).
     */
    public static boolean isCefThread() {
        return N_IsOnCefThread(TID_UI);
    }

    private static final native boolean N_PostTask(int thread_id, CefTask task);
    private static final native boolean N_IsOnCefThread(int thread_id);
}
