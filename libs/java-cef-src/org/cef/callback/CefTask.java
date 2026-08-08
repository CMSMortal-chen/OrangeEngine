// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.callback;

/**
 * Interface representing a task that can be posted to a CEF thread via
 * CefTaskRunner.postTask. Used to marshal calls to the CEF UI thread when
 * multi_threaded_message_loop is enabled.
 */
public interface CefTask {
    /**
     * Method that will be executed on the target thread.
     */
    void execute();
}
