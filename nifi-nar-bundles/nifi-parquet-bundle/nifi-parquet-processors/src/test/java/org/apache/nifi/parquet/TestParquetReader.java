/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.parquet;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.serialization.MalformedRecordException;
import org.apache.nifi.serialization.RecordReader;
import org.apache.nifi.util.MockComponentLog;
import org.apache.nifi.util.MockConfigurationContext;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisabledOnOs({ OS.WINDOWS })
public class TestParquetReader {

    private static final String PARQUET_PATH = "src/test/resources/TestParquetReader.parquet";
    private static final String SCHEMA_PATH = "src/test/resources/avro/user.avsc";

    private ParquetReader parquetReaderFactory;
    private ComponentLog componentLog;

    @BeforeEach
    public void setup() {
        Map<PropertyDescriptor, String> readerFactoryProperties = new HashMap<>();
        ConfigurationContext readerFactoryConfigContext = new MockConfigurationContext(readerFactoryProperties, null, null);

        parquetReaderFactory = new ParquetReader();
        parquetReaderFactory.abstractStoreConfigContext(readerFactoryConfigContext);

        componentLog = new MockComponentLog("1234", parquetReaderFactory);
    }

    @Test
    public void testReadUsers() throws IOException, MalformedRecordException {
        final Schema schema = getSchema();
        final File parquetFile = new File("target/TestParquetReader-testReadUsers-" + System.currentTimeMillis());

        // write some users to the parquet file...
        final int numUsers = 10;
        try (final ParquetWriter<GenericRecord> writer = createParquetWriter(schema, parquetFile)) {
            for (int i=0; i < numUsers; i++) {
                final GenericRecord user = new GenericData.Record(schema);
                user.put("name", "Bob" + i);
                user.put("favorite_number", i);
                user.put("favorite_color", "blue" + i);
                writer.write(user);
            }
        }

        // read the parquet file into bytes since we can't use a FileInputStream since it doesn't support mark/reset
        final byte[] parquetBytes = IOUtils.toByteArray(parquetFile.toURI());

        // read the users in using the record reader...
        try (final InputStream in = new ByteArrayInputStream(parquetBytes);
             final RecordReader recordReader = parquetReaderFactory.createRecordReader(
                     Collections.emptyMap(), in, parquetFile.length(), componentLog)) {

            int recordCount = 0;
            while (recordReader.nextRecord() != null) {
                recordCount++;
            }
            assertEquals(numUsers, recordCount);
        }
    }

    @Test
    public void testReader() throws InitializationException, IOException  {
        final TestRunner runner = TestRunners.newTestRunner(TestParquetProcessor.class);


        final ParquetReader parquetReader = new ParquetReader();

        runner.addControllerService("reader", parquetReader);
        runner.enableControllerService(parquetReader);

        runner.enqueue(Paths.get(PARQUET_PATH));

        runner.setProperty(TestParquetProcessor.READER, "reader");
        runner.setProperty(TestParquetProcessor.PATH, PARQUET_PATH);

        runner.run();
        runner.assertAllFlowFilesTransferred(TestParquetProcessor.SUCCESS, 1);
    }


    private Schema getSchema() throws IOException {
        final File schemaFile = new File(SCHEMA_PATH);
        final String schemaString = IOUtils.toString(new FileInputStream(schemaFile), StandardCharsets.UTF_8);
        return new Schema.Parser().parse(schemaString);
    }

    private ParquetWriter<GenericRecord> createParquetWriter(final Schema schema, final File parquetFile) throws IOException {
        final Configuration conf = new Configuration();
        final Path parquetPath = new Path(parquetFile.getPath());

        return AvroParquetWriter.<GenericRecord>builder(HadoopOutputFile.fromPath(parquetPath, conf))
                        .withSchema(schema)
                        .withConf(conf)
                        .build();
    }
}
