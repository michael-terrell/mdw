/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bare min impl to support CLI without dependencies.
 */
public class Unzip {

    private static final int BUFFER_KB = 16;

    private File zipFile;
    private File destDir;
    private boolean overwrite;

    public Unzip(File zipFile, File destDir) {
        this(zipFile, destDir, false);
    }

    public Unzip(File zipFile, File destDir, boolean overwrite) {
        this.zipFile = zipFile;
        this.destDir = destDir;
        this.overwrite = overwrite;
    }

    public void run() throws IOException {
        if (!destDir.exists() || !destDir.isDirectory())
            throw new IOException("Destination directory does not exist: " + destDir);
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String outpath = destDir + "/" + entry.getName();
                File outfile = new File(outpath);
                if (outfile.exists() && !overwrite)
                    throw new IOException("Destination already exists: " + outfile.getAbsolutePath());
                if (entry.isDirectory()) {
                    if (outfile.exists())
                        new Delete(outfile).run();
                    Files.createDirectories(Paths.get(outfile.getPath()));
                }
                else {
                    InputStream is = null;
                    OutputStream os = null;
                    try {
                        is = zip.getInputStream(entry);
                        os = new FileOutputStream(outfile);
                        int read = 0;
                        byte[] bytes = new byte[BUFFER_KB * 1024];
                        while((read = is.read(bytes)) != -1)
                            os.write(bytes, 0, read);
                    }
                    finally {
                        if (is != null)
                            is.close();
                        if (os != null)
                          os.close();
                    }
                }
            }
        }
    }
}