/*
 * Copyright (C) 2016 Jason von Nieda <jason@vonnieda.org>
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

package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Action;

import org.openpnp.CameraListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.OnvifIPCameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.soap.SOAPException;

import org.onvif.ver10.schema.JpegOptions;
import org.onvif.ver10.schema.Profile;
import org.onvif.ver10.schema.VideoEncoderConfiguration;
import org.onvif.ver10.schema.VideoEncoderConfigurationOptions;
import org.onvif.ver10.schema.VideoEncoding;
import org.onvif.ver10.schema.VideoResolution;

import de.onvif.soap.OnvifDevice;
import de.onvif.soap.devices.InitialDevices;
import de.onvif.soap.devices.MediaDevices;

/**
 * A Camera implementation for ONVIF compatible IP cameras.
 */
public class OnvifIPCamera extends ReferenceCamera implements Runnable {

    @Attribute(required = false)
    private int preferredWidth;
    @Attribute(required = false)
    private int preferredHeight;
    @Attribute(required = false)
    private int fps = 24;

    private Thread thread;
    private boolean dirty = false;
    
    @Attribute(required = false)
    private String hostIP;
    @Attribute(required = false)
    private String username;
    @Attribute(required = false)
    private String password;
    
    private URL snapshotURI;

	public OnvifIPCamera() {}

	@Override
	public BufferedImage capture() {
        if (thread == null) {
            initCamera();
        }
        try {
    		if (snapshotURI == null) {
    			return null;
    		}
            BufferedImage img = ImageIO.read(snapshotURI);
            return transformImage(img);
        }
        catch (Exception e) {
        	e.printStackTrace();
            return null;
        }
	}

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        if (thread == null) {
            initCamera();
        }
        super.startContinuousCapture(listener, maximumFps);
    }

    public void run() {
        while (!Thread.interrupted()) {
            try {
                BufferedImage image = capture();
                if (image != null) {
                    broadcastCapture(image);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000 / fps);
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void initCamera() {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            thread = null;
        }
        try {
            setDirty(false);
            width = null;
            height = null;
            snapshotURI = null;
            
            if ((hostIP != null) && (!hostIP.isEmpty())) {
				try {
					OnvifDevice nvt;
					if ((username != null) && (!username.isEmpty())) {
					   nvt = new OnvifDevice(hostIP, username, password);
					} else {
					   nvt = new OnvifDevice(hostIP);
					}
					
					InitialDevices devices = nvt.getDevices();
					List<Profile> profiles = devices.getProfiles();
					Profile profile = profiles.get(0);
					String profileToken = profile.getToken();
					MediaDevices media = nvt.getMedia();

					VideoEncoderConfigurationOptions videoEncoderConfigurationOptions = media.getVideoEncoderConfigurationOptions(profileToken);
					JpegOptions jpegOptions = videoEncoderConfigurationOptions.getJPEG();
					List<VideoResolution> jpegResolutions = jpegOptions.getResolutionsAvailable();
					int maxRes = -1;
					int selectedResolutionIndex = -1;
					// Step 1: Select the highest resolution available
					System.out.println("Supported JPEG resolutions:");
					for (int i=0; i<jpegResolutions.size(); i++) {
						VideoResolution jpegResolution = jpegResolutions.get(i);
						
						int res = jpegResolution.getWidth() * jpegResolution.getHeight();
						if (res > maxRes) {
							maxRes = res;
							selectedResolutionIndex = i;
						}
						
						System.out.println("    " + jpegResolution.getWidth() + "x" + jpegResolution.getHeight());
					}
					// Step 2: If there's a preferredWidth or preferredHeight specified, select that (if it exists) instead
					if ((preferredWidth != 0) || (preferredHeight != 0)) {
						for (int i=0; i<jpegResolutions.size(); i++) {
							VideoResolution jpegResolution = jpegResolutions.get(i);
							
							if (preferredWidth != 0) {
								if (preferredHeight != 0) {
									if ((jpegResolution.getWidth() == preferredWidth) && (jpegResolution.getHeight() == preferredHeight)) {
										selectedResolutionIndex = i;
										break;
									}
								} else if (jpegResolution.getWidth() == preferredWidth) {
									selectedResolutionIndex = i;
									break;
								}
							} else {
								if (jpegResolution.getHeight() == preferredHeight) {
									selectedResolutionIndex = i;
									break;
								}
							}
						}
					}
					
					if (selectedResolutionIndex >= 0) {
						VideoEncoderConfiguration videoEncoderConfiguration = MediaDevices.getVideoEncoderConfiguration(profile);
						
						videoEncoderConfiguration.setEncoding(VideoEncoding.JPEG);
	
						VideoResolution jpegResolution = jpegResolutions.get(selectedResolutionIndex);
						videoEncoderConfiguration.setResolution(jpegResolution);
						System.out.println(" -> Selected " + jpegResolution.getWidth() + "x" + jpegResolution.getHeight());
						
						media.setVideoEncoderConfiguration(videoEncoderConfiguration);
					}
	
					snapshotURI = new URL(media.getSnapshotUri(profileToken));
					System.out.println("Snapshot URI: " + snapshotURI.toString());
				} catch (ConnectException e) {
					System.err.println("Could not connect to IP camera at " + hostIP + ".");
				} catch (SOAPException e) {
					e.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {

            }
        }
    }

    public String getHostIP() {
    	return hostIP;
    }
    
    public synchronized void setHostIP(String hostIP) {
    	this.hostIP = hostIP;
        setDirty(true);
    	
    	initCamera();
    }
    
    public String getUsername() {
    	return username;
    }
    
    public synchronized void setUsername(String username) {
    	this.username = username;
        setDirty(true);
    }
    
    public String getPassword() {
    	return password;
    }
    
    public synchronized void setPassword(String password) {
    	this.password = password;
        setDirty(true);
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
        setDirty(true);
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = preferredHeight;
        setDirty(true);
    }
    
    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
	public Wizard getConfigurationWizard() {
    	return new OnvifIPCameraConfigurationWizard(this);
	}

	@Override
	public String getPropertySheetHolderTitle() {
		return getClass().getSimpleName() + " " + getName();
	}

	@Override
	public PropertySheetHolder[] getChildPropertySheetHolders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new CameraConfigurationWizard(this)),
                new PropertySheetWizardAdapter(getConfigurationWizard())};
	}

	@Override
	public Action[] getPropertySheetHolderActions() {
		// TODO Auto-generated method stub
		return null;
	}
}
