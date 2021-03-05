/*
 * Copyright 2019 the original author or authors.
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
package org.acme.i18876;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import com.azure.storage.file.datalake.models.FileSystemItem;

public class ReproduceIssue18876Test {
    static final int randomSuffix = (int) (Math.random() * 10000);
    static final String fsName = "testfs" + randomSuffix;

    static DataLakeServiceClient client;
    static DataLakeFileClient fClient;

    @BeforeAll
    static void beforeAll() {
        StorageSharedKeyCredential creds = credentials();
        String endpoint = "https://" + creds.getAccountName() + ".dfs.core.windows.net";
        client = new DataLakeServiceClientBuilder()
                .credential(creds)
                .endpoint(endpoint)
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .buildClient();

    }

    @Test
    void i18876() throws IOException {

        Assertions.assertThat(client.listFileSystems().stream().map(FileSystemItem::getName)).doesNotContain(fsName);

        client.createFileSystem(fsName);

        Assertions.assertThat(client.listFileSystems().stream().map(FileSystemItem::getName)).contains(fsName);

        client.deleteFileSystem(fsName);

        Assertions.assertThat(client.listFileSystems().stream().map(FileSystemItem::getName)).doesNotContain(fsName);

    }

    static StorageSharedKeyCredential credentials() {
        final String azureStorageAccountName = Objects.requireNonNull(System.getenv("AZURE_STORAGE_ACCOUNT_NAME"),
                "Set AZURE_STORAGE_ACCOUNT_NAME env var");
        final String azureStorageAccountKey = Objects.requireNonNull(System.getenv("AZURE_STORAGE_ACCOUNT_KEY"),
                "Set AZURE_STORAGE_ACCOUNT_KEY env var");

        return new StorageSharedKeyCredential(azureStorageAccountName, azureStorageAccountKey);

    }

    static class SkipLastNewlineInputStream extends InputStream {

        private final BufferedInputStream delegate;

        public SkipLastNewlineInputStream(InputStream delegate) {
            super();
            this.delegate = delegate instanceof BufferedInputStream ? (BufferedInputStream) delegate
                    : new BufferedInputStream(delegate);
        }

        public boolean markSupported() {
            return false;
        }

        @Override
        public int read() throws IOException {
            int c = delegate.read();
            if (c < 0) {
                return -1;
            } else if (c == '\n') {
                /* look ahead */
                delegate.mark(1);
                int nextC = delegate.read();
                if (nextC < 0) {
                    /* \n is the last byte */
                    return -1;
                }
                delegate.reset();
            }
            return c;
        }

    }

}
