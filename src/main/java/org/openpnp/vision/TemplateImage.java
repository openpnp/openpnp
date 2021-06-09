/*
 * Copyright (C) 2021 <mark@makr.zone>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.vision;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.codec.digest.DigestUtils;
import org.openpnp.model.Configuration;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * A simple wrapper for a vision template image. This will make sure only one instance is maintained
 * and persisted even if it is referenced many times by different OpenPnP objects. Also provides
 * transparent XML serialization, so a TempateImage can simply be added as an @Element.
 * 
 */
public class TemplateImage {
    //TODO: perhaps support serialization inside the XML (base64 encoded).
    private final static String FILE_ENCODING_TYPE = "png";

    private static class ImageSlot {
        final private String hash;
        final private BufferedImage image;
        final private byte[] filedata;
        private boolean dirty;

        public ImageSlot(String hash, BufferedImage image, byte[] filedata, boolean dirty) {
            this.hash = hash;
            this.image = image;
            this.filedata = filedata;
            this.dirty = dirty;
        }

        public ImageSlot(String hash) throws IOException {
            File file = getFile(hash);
            byte[] filedata = Files.readAllBytes(file.toPath());
            ByteArrayInputStream stream = new ByteArrayInputStream(filedata);
            BufferedImage image = ImageIO.read(stream);
            this.hash = hash;
            this.image = image;
            this.filedata = filedata;
            this.dirty = false;
        }

        protected File getFile(String hash) throws IOException {
            return Configuration.get()
                                .getResourceFile(TemplateImage.class,
                                        hash + "." + FILE_ENCODING_TYPE);
        }

        public BufferedImage getImage() {
            return image;
        }

        public void persist() throws IOException {
            if (dirty) {
                File file = getFile(hash);
                Files.write(file.toPath(), filedata);
                dirty = false;
            }
        }
    }

    private static Map<String, ImageSlot> imageRegister = new HashMap<>();

    @Attribute
    private String hash;

    public TemplateImage() {}
    
    public TemplateImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, FILE_ENCODING_TYPE, outputStream);
        byte[] filedata = outputStream.toByteArray();
        hash = DigestUtils.shaHex(filedata);
        if (!imageRegister.containsKey(hash)) {
            // We re-read the file from the compressed filedata. 
            // This way we also get rid of subImage -> OpenCv Mat incompatibility. 
            ByteArrayInputStream stream = new ByteArrayInputStream(filedata);
            image = ImageIO.read(stream);
            imageRegister.put(hash, new ImageSlot(hash, image, filedata, true));
        }
    }

    @Commit
    private void commit() throws IOException {
        if (!imageRegister.containsKey(hash)) {
            imageRegister.put(hash, new ImageSlot(hash));
        }
    }

    @Persist
    private void persist() throws IOException {
        imageRegister.get(hash)
                     .persist();
    }

    public BufferedImage getImage() throws Exception {
        ImageSlot slot = imageRegister.get(hash);
        if (slot == null) {
            throw new Exception("Template Image is missing");
        }
        return slot.getImage();
    }
}
