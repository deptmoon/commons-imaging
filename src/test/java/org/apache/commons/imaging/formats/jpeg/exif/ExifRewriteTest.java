/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.imaging.formats.jpeg.exif;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.IImageMetadata.IImageMetadataItem;
import org.apache.commons.imaging.common.bytesource.ByteSource;
import org.apache.commons.imaging.common.bytesource.ByteSourceArray;
import org.apache.commons.imaging.common.bytesource.ByteSourceFile;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegUtils;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.AllTagConstants;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldType;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.imaging.util.Debug;
import org.apache.commons.imaging.util.IoUtils;

public class ExifRewriteTest extends ExifBaseTest implements AllTagConstants {
    // public ExifRewriteTest(String name)
    // {
    // super(name);
    // }

    public void testRemove() throws Exception {
        final List<File> images = getImagesWithExifData();
        for (int i = 0; i < images.size(); i++) {
            if (i % 10 == 0) {
                Debug.purgeMemory();
            }

            final File imageFile = images.get(i);
            Debug.debug("imageFile", imageFile);

            final boolean ignoreImageData = isPhilHarveyTestImage(imageFile);
            if (ignoreImageData) {
                continue;
            }

            final ByteSource byteSource = new ByteSourceFile(imageFile);
            Debug.debug("Source Segments:");
            new JpegUtils().dumpJFIF(byteSource);

            {
                final JpegImageMetadata metadata = (JpegImageMetadata) Imaging
                        .getMetadata(imageFile);
                // assertNotNull(metadata.getExif());
            }

            {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new ExifRewriter().removeExifMetadata(byteSource, baos);
                final byte bytes[] = baos.toByteArray();
                final File tempFile = createTempFile("test", ".jpg");
                Debug.debug("tempFile", tempFile);
                IoUtils.writeToFile(bytes, tempFile);

                Debug.debug("Output Segments:");
                new JpegUtils().dumpJFIF(new ByteSourceArray(bytes));

                assertTrue(!hasExifData(tempFile));
            }
        }
    }

    public void testInsert() throws Exception {
        final List<File> images = getImagesWithExifData();
        for (int i = 0; i < images.size(); i++) {
            if (i % 10 == 0) {
                Debug.purgeMemory();
            }

            final File imageFile = images.get(i);
            Debug.debug("imageFile", imageFile);

            final boolean ignoreImageData = isPhilHarveyTestImage(imageFile);
            if (ignoreImageData) {
                continue;
            }

            final ByteSource byteSource = new ByteSourceFile(imageFile);
            Debug.debug("Source Segments:");
            new JpegUtils().dumpJFIF(byteSource);

            final JpegImageMetadata originalMetadata = (JpegImageMetadata) Imaging
                    .getMetadata(imageFile);
            assertNotNull(originalMetadata);

            final TiffImageMetadata oldExifMetadata = originalMetadata.getExif();
            assertNotNull(oldExifMetadata);

            ByteSource stripped;
            {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new ExifRewriter().removeExifMetadata(byteSource, baos);
                final byte bytes[] = baos.toByteArray();
                final File tempFile = createTempFile("removed", ".jpg");
                Debug.debug("tempFile", tempFile);
                IoUtils.writeToFile(bytes, tempFile);

                Debug.debug("Output Segments:");
                stripped = new ByteSourceArray(bytes);
                new JpegUtils().dumpJFIF(stripped);

                assertTrue(!hasExifData(tempFile));
            }

            {
                final TiffOutputSet outputSet = oldExifMetadata.getOutputSet();
                // outputSet.dump();

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                new ExifRewriter().updateExifMetadataLossy(stripped, baos,
                        outputSet);

                final byte bytes[] = baos.toByteArray();
                final File tempFile = createTempFile("inserted" + "_", ".jpg");
                Debug.debug("tempFile", tempFile);
                IoUtils.writeToFile(bytes, tempFile);

                Debug.debug("Output Segments:");
                new JpegUtils().dumpJFIF(new ByteSourceArray(bytes));

                // assertTrue(!hasExifData(tempFile));

                final JpegImageMetadata newMetadata = (JpegImageMetadata) Imaging
                        .getMetadata(tempFile);
                assertNotNull(newMetadata);
                final TiffImageMetadata newExifMetadata = newMetadata.getExif();
                assertNotNull(newExifMetadata);
                // newMetadata.dump();

                compare(imageFile, oldExifMetadata, newExifMetadata);
            }

        }
    }

    private interface Rewriter {
        public void rewrite(ByteSource byteSource, OutputStream os,
                TiffOutputSet outputSet) throws ImageReadException,
                IOException, ImageWriteException;
    }

    private void rewrite(final Rewriter rewriter, final String name) throws IOException,
            ImageReadException, ImageWriteException {
        final List<File> images = getImagesWithExifData();
        for (int i = 0; i < images.size(); i++) {
            if (i % 10 == 0) {
                Debug.purgeMemory();
            }

            final File imageFile = images.get(i);

            try {

                Debug.debug("imageFile", imageFile);

                final boolean ignoreImageData = isPhilHarveyTestImage(imageFile);
                if (ignoreImageData) {
                    continue;
                }

                final ByteSource byteSource = new ByteSourceFile(imageFile);
                Debug.debug("Source Segments:");
                new JpegUtils().dumpJFIF(byteSource);

                final JpegImageMetadata oldMetadata = (JpegImageMetadata) Imaging
                        .getMetadata(imageFile);
                if (null == oldMetadata) {
                    continue;
                }
                assertNotNull(oldMetadata);

                final TiffImageMetadata oldExifMetadata = oldMetadata.getExif();
                if (null == oldExifMetadata) {
                    continue;
                }
                assertNotNull(oldExifMetadata);
                oldMetadata.dump();

                // TiffImageMetadata tiffImageMetadata = metadata.getExif();
                // Photoshop photoshop = metadata.getPhotoshop();

                final TiffOutputSet outputSet = oldExifMetadata.getOutputSet();
                // outputSet.dump();

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rewriter.rewrite(byteSource, baos, outputSet);
                final byte bytes[] = baos.toByteArray();
                final File tempFile = createTempFile(name + "_", ".jpg");
                Debug.debug("tempFile", tempFile);
                IoUtils.writeToFile(bytes, tempFile);

                Debug.debug("Output Segments:");
                new JpegUtils().dumpJFIF(new ByteSourceArray(bytes));

                // assertTrue(!hasExifData(tempFile));

                final JpegImageMetadata newMetadata = (JpegImageMetadata) Imaging
                        .getMetadata(tempFile);
                assertNotNull(newMetadata);
                final TiffImageMetadata newExifMetadata = newMetadata.getExif();
                assertNotNull(newExifMetadata);
                // newMetadata.dump();

                compare(imageFile, oldExifMetadata, newExifMetadata);
            } catch (final IOException e) {
                Debug.debug("imageFile", imageFile.getAbsoluteFile());
                Debug.debug(e);
                throw e;
            } catch (final ImageReadException e) {
                Debug.debug("imageFile", imageFile.getAbsoluteFile());
                Debug.debug(e);
                throw e;
            } catch (final ImageWriteException e) {
                Debug.debug("imageFile", imageFile.getAbsoluteFile());
                Debug.debug(e);
            }

        }
    }

    public void testRewriteLossy() throws Exception {
        final Rewriter rewriter = new Rewriter() {
            public void rewrite(final ByteSource byteSource, final OutputStream os,
                    final TiffOutputSet outputSet) throws ImageReadException,
                    IOException, ImageWriteException {
                new ExifRewriter().updateExifMetadataLossy(byteSource, os,
                        outputSet);
            }
        };

        rewrite(rewriter, "lossy");
    }

    public void testRewriteLossless() throws Exception {
        final Rewriter rewriter = new Rewriter() {
            public void rewrite(final ByteSource byteSource, final OutputStream os,
                    final TiffOutputSet outputSet) throws ImageReadException,
                    IOException, ImageWriteException {
                new ExifRewriter().updateExifMetadataLossless(byteSource, os,
                        outputSet);
            }
        };

        rewrite(rewriter, "lossless");
    }

    private Hashtable<Integer,TiffImageMetadata.Directory> makeDirectoryMap(final List<? extends IImageMetadataItem> directories) {
        final Hashtable<Integer,TiffImageMetadata.Directory> directoryMap = new Hashtable<Integer,TiffImageMetadata.Directory>();
        for (int i = 0; i < directories.size(); i++) {
            final TiffImageMetadata.Directory directory = (TiffImageMetadata.Directory) directories
                    .get(i);
            directoryMap.put(directory.type, directory);
        }
        return directoryMap;
    }

    private Hashtable<Integer,TiffField> makeFieldMap(final List<? extends IImageMetadataItem> items) {
        final Hashtable<Integer,TiffField> fieldMap = new Hashtable<Integer,TiffField>();
        for (int i = 0; i < items.size(); i++) {
            final TiffImageMetadata.Item item = (TiffImageMetadata.Item) items.get(i);
            final TiffField field = item.getTiffField();
            if (!fieldMap.containsKey(field.getTag())) {
                fieldMap.put(field.getTag(), field);
            }
        }
        return fieldMap;
    }

    private void compare(final File imageFile, final TiffImageMetadata oldExifMetadata,
            final TiffImageMetadata newExifMetadata) throws ImageReadException {
        assertNotNull(oldExifMetadata);
        assertNotNull(newExifMetadata);

        final List<? extends IImageMetadataItem> oldDirectories = oldExifMetadata.getDirectories();
        final List<? extends IImageMetadataItem> newDirectories = newExifMetadata.getDirectories();

        assertTrue(oldDirectories.size() == newDirectories.size());

        final Hashtable<Integer,TiffImageMetadata.Directory> oldDirectoryMap = makeDirectoryMap(oldDirectories);
        final Hashtable<Integer,TiffImageMetadata.Directory> newDirectoryMap = makeDirectoryMap(newDirectories);

        assertEquals(oldDirectories.size(), oldDirectoryMap.keySet().size());
        final List<Integer> oldDirectoryTypes = new ArrayList<Integer>(oldDirectoryMap.keySet());
        Collections.sort(oldDirectoryTypes);
        final List<Integer> newDirectoryTypes = new ArrayList<Integer>(newDirectoryMap.keySet());
        Collections.sort(newDirectoryTypes);
        assertEquals(oldDirectoryTypes, newDirectoryTypes);

        for (int i = 0; i < oldDirectoryTypes.size(); i++) {
            final Integer dirType = oldDirectoryTypes.get(i);

            // Debug.debug("dirType", dirType);

            final TiffImageMetadata.Directory oldDirectory = oldDirectoryMap
                    .get(dirType);
            final TiffImageMetadata.Directory newDirectory = newDirectoryMap
                    .get(dirType);
            assertNotNull(oldDirectory);
            assertNotNull(newDirectory);

            final List<? extends IImageMetadataItem> oldItems = oldDirectory.getItems();
            final List<? extends IImageMetadataItem> newItems = newDirectory.getItems();

            // Debug.debug("oldItems.size()", oldItems.size());
            // Debug.debug("newItems.size()", newItems.size());
            // dump("oldItems", oldItems);
            // dump("newItems", newItems);

            // if (oldItems.size() != newItems.size())
            // ;
            // {
            // dump("oldItems", oldItems);
            // dump("newItems", newItems);
            // }
            // assertTrue(oldItems.size() == newItems.size());

            final Hashtable<Integer,TiffField> oldFieldMap = makeFieldMap(oldItems);
            final Hashtable<Integer,TiffField> newFieldMap = makeFieldMap(newItems);

            final Set<Integer> missingInNew = new HashSet<Integer>(oldFieldMap.keySet());
            missingInNew.removeAll(newFieldMap.keySet());

            final Set<Integer> missingInOld = new HashSet<Integer>(newFieldMap.keySet());
            missingInOld.removeAll(oldFieldMap.keySet());

            // dump("missingInNew", missingInNew);
            // dump("missingInOld", missingInOld);
            // dump("newFieldMap.keySet()", newFieldMap.keySet());
            // dump("oldFieldMap.keySet()", oldFieldMap.keySet());

            assertTrue(missingInNew.size() == 0);
            assertTrue(missingInOld.size() == 0);

            // Debug.debug("oldItems.size()", oldItems.size());
            // Debug.debug("oldFieldMap.keySet().size()",
            // oldFieldMap.keySet().size());

            // assertEquals(oldItems.size(), oldFieldMap.keySet().size());
            // assertEquals(oldFieldMap.keySet(), newFieldMap.keySet());
            // assertEquals(oldFieldMap.keySet(), newFieldMap.keySet());

            final List<Integer> oldFieldTags = new ArrayList<Integer>(oldFieldMap.keySet());
            Collections.sort(oldFieldTags);
            final List<Integer> newFieldTags = new ArrayList<Integer>(newFieldMap.keySet());
            Collections.sort(newFieldTags);
            assertEquals(oldFieldTags, newFieldTags);

            for (int j = 0; j < oldFieldTags.size(); j++) {
                final Integer fieldTag = oldFieldTags.get(j);

                final TiffField oldField = oldFieldMap.get(fieldTag);
                final TiffField newField = newFieldMap.get(fieldTag);

                // Debug.debug("fieldTag", fieldTag);
                // Debug.debug("oldField", oldField);
                // Debug.debug("newField", newField);

                // fieldTag.
                assertNotNull(oldField);
                assertNotNull(newField);

                assertEquals(oldField.getTag(), newField.getTag());
                assertEquals(dirType.intValue(), newField.getDirectoryType());
                assertEquals(oldField.getDirectoryType(), newField.getDirectoryType());

                if (oldField.getFieldType() == FieldType.ASCII) {
                    // Imaging currently doesn't correctly rewrite
                    // strings if any byte had the highest bit set,
                    // so if the source had that, all bets are off.
                    final byte[] rawBytes = oldField.getByteArrayValue();
                    boolean hasInvalidByte = false;
                    for (final byte rawByte : rawBytes) {
                        if ((rawByte & 0x80) != 0) {
                            hasInvalidByte = true;
                            break;
                        }
                    }
                    if (hasInvalidByte) {
                        continue;
                    }
                }

                assertEquals(oldField.getCount(), newField.getCount());
                assertEquals(oldField.isLocalValue(), newField.isLocalValue());

                if (oldField.getTag() == 0x202) {
                    // ignore "jpg from raw length" value. may have off-by-one
                    // bug in certain cameras.
                    // i.e. Sony DCR-PC110
                    continue;
                }

                if (!oldField.getTagInfo().isOffset()) {
                    if (oldField.getTagInfo().isText()) { /* do nothing */
                    } else if (oldField.isLocalValue()) {
                        // Debug.debug("oldField.tag", oldField.tag);
                        // Debug.debug("newField.tag", newField.tag);
                        // Debug.debug("oldField.tagInfo", oldField.tagInfo);
                        // Debug.debug("newField.tagInfo", newField.tagInfo);
                        // Debug.debug("oldField.fieldType",
                        // oldField.fieldType);
                        // Debug.debug("newField.fieldType",
                        // newField.fieldType);
                        // Debug.debug("oldField.getBytesLength", oldField
                        // .getBytesLength());
                        // Debug.debug("newField.getBytesLength", newField
                        // .getBytesLength());
                        //
                        // Debug.debug("oldField.valueOffsetBytes",
                        // oldField.valueOffsetBytes);
                        // Debug.debug("newField.valueOffsetBytes",
                        // newField.valueOffsetBytes);

                        final String label = imageFile.getName() + ", dirType[" + i
                                + "]=" + dirType + ", fieldTag[" + j + "]="
                                + fieldTag;
                        if (oldField.getTag() == 0x116 || oldField.getTag() == 0x117) {
                            compare(label, oldField, newField);
                        } else {
                            compare(label, oldField.getByteArrayValue(),
                                    newField.getByteArrayValue(),
                                    oldField.getBytesLength(),
                                    newField.getBytesLength());
                        }
                    } else {
                        // Debug.debug("oldField.tagInfo", oldField.tagInfo);
                        // Debug.debug("oldField.fieldType",
                        // oldField.fieldType);
                        // Debug.debug("newField.fieldType",
                        // newField.fieldType);
                        // Debug.debug("oldField.getBytesLength", oldField
                        // .getBytesLength());
                        // Debug.debug("newField.getBytesLength", newField
                        // .getBytesLength());

                        // Debug.debug("oldField.oversizeValue",
                        // oldField.oversizeValue);
                        // Debug.debug("newField.oversizeValue",
                        // newField.oversizeValue);

                        compare(oldField.getByteArrayValue(), newField.getByteArrayValue());
                    }
                }

            }

            // Debug.debug();
        }
    }

    private void compare(final String label, final byte a[], final byte b[], final int aLength,
            final int bLength) {
        // Debug.debug("c0 a", a);
        // Debug.debug("c0 b", b);
        assertEquals(aLength, bLength);
        assertTrue(a.length >= aLength);
        assertTrue(b.length >= bLength);
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a.length, b.length);
        final int length = aLength;
        for (int i = 0; i < length; i++) {
            // byte ba = a[i];
            // byte bb = b[i];
            // boolean eq = ba == bb;
            // Debug.debug("i: " + i + ", a[i]: " + ba + ", b[i]: " + bb + " = "
            // + (ba == bb) + " " + eq);
            // if(ba != bb)
            // assertFalse(true);
            //
            // Debug.debug("i: " + i + ", a[i]: " + ba + ", b[i]: " + bb + " = "
            // + (ba == bb) + " " + eq);
            // assertTrue(eq == true);
            assertEquals(label + ", byte[" + i + "]", a[i], b[i]);
            // Debug.debug("c");
            // assertTrue((0xff & a[i]) == (0xff & b[i]));
        }
    }

    private void compare(final String label, final TiffField a, final TiffField b)
            throws ImageReadException {
        final Object v1 = a.getValue();
        final Object v2 = b.getValue();

        // Debug.debug("v1", v1 + " (" + Debug.getType(v1) + ")");
        // Debug.debug("v2", v2 + " (" + Debug.getType(v2) + ")");
        assertEquals(label, v1, v2);
    }

    private void compare(final byte a[], final byte b[]) {
        // Debug.debug("c1 a", a);
        // Debug.debug("c1 b", b);
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }
    }
}
