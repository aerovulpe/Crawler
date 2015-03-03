/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package me.aerovulpe.crawler;

/**
 * Some global configuration parameters.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class PicViewConfig {

    /**
     * Used for storing files on the file system as a directory.
     */
    public static final String APP_NAME_PATH = "picview";
    /**
     * The size of the album thumbnails (in dp).
     */
    public static int ALBUM_THUMBNAIL_SIZE = 140;

    private PicViewConfig() {
    }
}