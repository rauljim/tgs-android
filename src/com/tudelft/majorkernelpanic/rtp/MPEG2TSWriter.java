/*
 * Copyright (C) 2010 The Android Open Source Project
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
 * 
 * Java translation and H.264 adjustments are 
 * Copyright (c) 2012 Technische Universiteit Delft.
 */
package com.tudelft.majorkernelpanic.rtp;

/*
 * Arno: MPEG2TSWriter taken from Android's libstagefright and modified such 
 * that it works with VLC's libdvbpsi decoder:
 *  - No htonl() on CRC32 in PAT and PMT
 *  - Proper PCR in MPEGTS data packets for video
 *  
 *  Note this is not a generic muxer for H.264 into MPEGTS, as the
 *  H.264 SPS and PPS constants are for 640x480 15 fps 500 kbps video. 
 */

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.tudelft.triblerdroid.swift.NativeLib;


public class MPEG2TSWriter
{
    public long mNumTSPacketsWritten = 0;
    public long mNumTSPacketsBeforeMeta = 0;
    public byte mPATContinuityCounter = 0;
    public byte mPMTContinuityCounter = 0;
    public byte mContinuityCounter = 0; // for video
    public int[]  mCrcTable;
    
    public NativeLib mNativeLib = null;
    public String mSwarmID = null;

    public byte[] mH264AccessUnitDelimiter = { 0x00, 0x00, 0x00, 0x01, 0x09, (byte)0xf0 };
    // 640x480 15 frames 500 kbps specific
    public byte[] mH264SequenceParamSet = { 0x00, 0x00, 0x00, 0x01, 0x27, 0x42, (byte)0x80, 0x29, (byte)0x8D, (byte)0x95, 0x01, 0x40, 0x7B, 0x20 };
    public byte[] mH264PictureParamSet = { 0x00, 0x00, 0x00, 0x01, 0x28, (byte)0xDE, 0x09, (byte)0x88 };

    //http://stackoverflow.com/questions/6199940/generate-pcr-from-pts
    public long PTSaheadOfPCRus = 500000; // 500 ms
    
    // Manual timing, hardcoded 15 fps. RTP timestamp from H.264Packetizer not ideal
    public long mTimeStampIncr = 66000; // us; 1000 ms / 15 fps = 66 ms / frame (48000 from ffprobe?)
    public long mTimeStamp = PTSaheadOfPCRus + 4000;
    
    public ByteArrayOutputStream mMpegtsBAOS; // for increasing granuality of calls to swift::LiveAdd
    
	public MPEG2TSWriter(String swarmid)
	{
		mCrcTable = new int[256];
		initCrcTable();
		mSwarmID =  swarmid;
		mNativeLib = new NativeLib();
		mMpegtsBAOS = new ByteArrayOutputStream();
    }

	byte ArnoVideoSourceIncrementContinuityCounter() 
	{
	    if (++mContinuityCounter == 16) {
	        mContinuityCounter = 0;
	    }

	    return mContinuityCounter;
	}

	public void writeProgramAssociationTable() throws IOException 
	{
	    // 0x47
	    // transport_error_indicator = b0
	    // payload_unit_start_indicator = b1
	    // transport_priority = b0
	    // PID = b0000000000000 (13 bits)
	    // transport_scrambling_control = b00
	    // adaptation_field_control = b01 (no adaptation field, payload only)
	    // continuity_counter = b????
	    // skip = 0x00
	    // --- payload follows
	    // table_id = 0x00
	    // section_syntax_indicator = b1
	    // must_be_zero = b0
	    // reserved = b11
	    // section_length = 0x00d
	    // transport_stream_id = 0x0000
	    // reserved = b11
	    // version_number = b00001
	    // current_next_indicator = b1
	    // section_number = 0x00
	    // last_section_number = 0x00
	    //   one program follows:
	    //   program_number = 0x0001
	    //   reserved = b111
	    //   program_map_PID = 0x01e0 (13 bits!)
	    // CRC = 0x????????
	
	    byte[] kData = {
	        0x47,
	        0x40, 0x00, 0x10, 0x00,  // b0100 0000 0000 0000 0001 ???? 0000 0000
	        0x00, (byte)0xb0, 0x0d, 0x00,  // b0000 0000 1011 0000 0000 1101 0000 0000
	        0x00, (byte)0xc3, 0x00, 0x00,  // b0000 0000 1100 0011 0000 0000 0000 0000
	        0x00, 0x01, (byte)0xe1, (byte)0xe0,  // b0000 0000 0000 0001 1110 0001 1110 0000
	        0x00, 0x00, 0x00, 0x00   // b???? ???? ???? ???? ???? ???? ???? ????
	    };
	    /*
		// MPEGTS
        0x47,
        0x40,
        0x00, 
        0x10, 
        // PAT
        0x00,  PTR
        0x00,  TABLE
        0xb0, SSI 0 RES
        0x0d, SECLEN
        0x00,  TSID0
        0x00,  TSID1
        0xc3, RES + VER + CURRENT
        0x00, SEC NO
        0x00,  LAST SEC NO
        0x00,  PROG0
        0x01,  PROG1
        0xe1, RES + 5-bit PID0
        0xe0, 8-bit PID1
        0x00, 
        0x00, 
        0x00, 
        0x00   // b???? ???? ???? ???? ???? ???? ???? ????
	    */
	    byte[] buffer = new byte[188];
	    for (int i=0; i<188; i++)
	    	buffer[i] = (byte)0xff;
	    System.arraycopy(kData,0,buffer,0,kData.length);
	    
	    buffer[3] |= mPATContinuityCounter;

	    if (++mPATContinuityCounter == 16) {
	        mPATContinuityCounter = 0;
	    }
	    
	    byte[] bcrc = crc32(buffer, 5, 12);
	    for (int i=0; i<4; i++)
	    	buffer[17+i] = bcrc[i]; 

	    output(buffer);
	}

	public void writeProgramMap() throws IOException 
	{
	    // 0x47
	    // transport_error_indicator = b0
	    // payload_unit_start_indicator = b1
	    // transport_priority = b0
	    // PID = b0 0001 1110 0000 (13 bits) [0x01e0]
	    // transport_scrambling_control = b00
	    // adaptation_field_control = b01 (no adaptation field, payload only)
	    // continuity_counter = b????
	    // skip = 0x00
	    // -- payload follows
	    // table_id = 0x02
	    // section_syntax_indicator = b1
	    // must_be_zero = b0
	    // reserved = b11
	    // section_length = 0x???
	    // program_number = 0x0001
	    // reserved = b11
	    // version_number = b00001
	    // current_next_indicator = b1
	    // section_number = 0x00
	    // last_section_number = 0x00
	    // reserved = b111
	    // PCR_PID = b? ???? ???? ???? (13 bits)
	    // reserved = b1111
	    // program_info_length = 0x000
	    //   one or more elementary stream descriptions follow:
	    //   stream_type = 0x??
	    //   reserved = b111
	    //   elementary_PID = b? ???? ???? ???? (13 bits)
	    //   reserved = b1111
	    //   ES_info_length = 0x000
	    // CRC = 0x????????
	
	    byte[] kData = {
	        0x47,
	        0x41, (byte)0xe0, 0x10, 0x00,  // b0100 0001 1110 0000 0001 ???? 0000 0000
	        0x02, (byte)0xb0, 0x00, 0x00,  // b0000 0010 1011 ???? ???? ???? 0000 0000
	        0x01, (byte)0xc3, 0x00, 0x00,  // b0000 0001 1100 0011 0000 0000 0000 0000
	        (byte)0xe0, 0x00, (byte)0xf0, 0x00   // b111? ???? ???? ???? 1111 0000 0000 0000
	    };

	    byte[] buffer = new byte[188];
	    for (int i=0; i<188; i++)
	    	buffer[i] = (byte)0xff;
	    System.arraycopy(kData,0,buffer,0,kData.length);
	
	    buffer[3] |= mPMTContinuityCounter;

	    if (++mPMTContinuityCounter == 16) {
	        mPMTContinuityCounter = 0;
	    }
	    
	    int numSources = 1; // mSources.size();
	    int section_length = 5 * numSources + 4 + 9;
	    buffer[6] |= (byte)(section_length >> 8);
	    buffer[7] = (byte)(section_length & 0xff);
	
	    short kPCR_PID = 0x01e1;
	    //short kPCR_PID = 0x01ff;
	    buffer[13] |= (byte)((kPCR_PID >> 8) & 0x1f);
	    buffer[14] = (byte)(kPCR_PID & 0xff);

	    
        byte streamType = 0x1b; // Arno: hardcode MEDIA_MIMETYPE_VIDEO_AVC. mSources.editItemAt(i)->streamType();
	    int pidx = kData.length;
	    for (short i = 0; i < numSources; ++i) {
	        buffer[pidx++] = streamType; 
	
	        short ES_PID = (short)(0x01e0 + i + 1);
	        buffer[pidx++] = (byte)(0xe0 | (ES_PID >> 8));
	        buffer[pidx++] = (byte)(ES_PID & 0xff);
	        buffer[pidx++] = (byte)0xf0;
	        buffer[pidx++] = 0x00;
	    }

	    
	    byte[] bcrc = crc32(buffer, 5, 12+numSources*5);
	    for (int i=0; i<4; i++)
	    	buffer[(17+numSources*5)+i] = bcrc[i];

	    output(buffer);
	}

	
	public void writeAccessUnit(byte[] accessUnit, long timeUs) throws IOException  
	{
	    // 0x47
	    // transport_error_indicator = b0
	    // payload_unit_start_indicator = b1
	    // transport_priority = b0
	    // PID = b0 0001 1110 ???? (13 bits) [0x01e0 + 1 + sourceIndex]
	    // transport_scrambling_control = b00
	    // adaptation_field_control = b??
	    // continuity_counter = b????
	    // -- payload follows
	    // packet_startcode_prefix = 0x000001
	    // stream_id = 0x?? (0xe0 for avc video, 0xc0 for aac audio)
	    // PES_packet_length = 0x????
	    // reserved = b10
	    // PES_scrambling_control = b00
	    // PES_priority = b0
	    // data_alignment_indicator = b1
	    // copyright = b0
	    // original_or_copy = b0
	    // PTS_DTS_flags = b10  (PTS only)
	    // ESCR_flag = b0
	    // ES_rate_flag = b0
	    // DSM_trick_mode_flag = b0
	    // additional_copy_info_flag = b0
	    // PES_CRC_flag = b0
	    // PES_extension_flag = b0
	    // PES_header_data_length = 0x05
	    // reserved = b0010 (PTS)
	    // PTS[32..30] = b???
	    // reserved = b1
	    // PTS[29..15] = b??? ???? ???? ???? (15 bits)
	    // reserved = b1
	    // PTS[14..0] = b??? ???? ???? ???? (15 bits)
	    // reserved = b1
	    // the first fragment of "buffer" follows
		
		short sourceIndex = 0; // Arno: hardcoded video
	
	    byte[] buffer = new byte[188];
	    for (int i=0; i<188; i++)
	    	buffer[i] = (byte)0xff;
	
	    short PID = (short)(0x01e0 + sourceIndex + 1);
	
	    byte continuity_counter = ArnoVideoSourceIncrementContinuityCounter();
	
	    // XXX if there are multiple streams of a kind (more than 1 audio or
	    // more than 1 video) they need distinct stream_ids.
	    
	    byte stream_id = (byte)0xe0; // Arno, hardcode video. mSources.editItemAt(sourceIndex)->streamType() == 0x0f ? 0xc0 : 0xe0;
	
	    // Arno: VLC complains about PCR wrap around even when PCR bit is not
	    // set in Adaptation field. Hence, set PCR. PCR calculated from PCR 
	    // as in:
	    // http://stackoverflow.com/questions/6199940/generate-pcr-from-pts
	    int PTS = (int)((timeUs * 9L) / 100L);
	    long PCR = PCR = (int)(((timeUs-PTSaheadOfPCRus) * 9L) / 100L);
    	PCR = PCR << 9L;

    	
	    int PES_packet_length = accessUnit.length + 8; // Arno: 8 = PES header (000001+sid not counted)
	    //boolean padding = (accessUnit.length < (188 - 18));
	    
	    boolean adaptation = (timeUs != 0); // ARNOTODO: do need padding via Adaptation if SPS and PPS sent as separate MPEGTS units
	    int offset = 0;
	    boolean lastAccessUnit = ((accessUnit.length - offset) < 184);
	    
	    if (PES_packet_length >= 65536) {
	        // It's valid to set this to 0 for video according to the specs.
	        PES_packet_length = 0;
	    }
	
	    int pidx = 0;
	    buffer[pidx++] = 0x47;
	    buffer[pidx++] = (byte)(0x40 | (PID >> 8));
	    buffer[pidx++] = (byte)(PID & 0xff);
	    buffer[pidx++] =  (byte)((adaptation ? 0x30 : 0x10) | continuity_counter);
    	
	    if (adaptation)
	    {
        	int paddingSize = 0;
        	if (lastAccessUnit)
        		paddingSize = 184 - (accessUnit.length - offset);
        	else
        		paddingSize = 8; // Arno PCR
            buffer[pidx++] = (byte)(paddingSize - 1); // Adaptation length
        	buffer[pidx++] = 0x10; // Adaptation bits: set PCR_flag
            buffer[pidx++] = (byte)(( PCR >> 34 ) & 0xff);
            buffer[pidx++] = (byte)(( PCR >> 26 ) & 0xff);
            buffer[pidx++] = (byte)(( PCR >> 18 ) & 0xff);
            buffer[pidx++] = (byte)(( PCR >> 10 ) & 0xff);
            buffer[pidx++] = (byte)(0x7e | ( ( PCR & (1<<9) ) >> 2 ) | ( ( PCR & (1<<8) ) >> 8 ));
            buffer[pidx++] = (byte)(PCR & 0xff);
            
        	if (lastAccessUnit)
        	{
        		pidx += paddingSize-8;
        	}
	    }

        buffer[pidx++] = 0x00;
	    buffer[pidx++] = 0x00;
	    buffer[pidx++] = 0x01;
	    buffer[pidx++] = stream_id;
	    buffer[pidx++] = (byte)(PES_packet_length >> 8);
	    buffer[pidx++] = (byte)(PES_packet_length & 0xff);
	    buffer[pidx++] = (byte)0x84;
	    buffer[pidx++] = (byte)0x80;
	    buffer[pidx++] = 0x05;
	    buffer[pidx++] = (byte)(0x20 | (((PTS >> 30) & 7) << 1) | 1);
	    buffer[pidx++] = (byte)((PTS >> 22) & 0xff);
	    buffer[pidx++] = (byte)((((PTS >> 15) & 0x7f) << 1) | 1);
	    buffer[pidx++] = (byte)((PTS >> 7) & 0xff);
	    buffer[pidx++] = (byte)(((PTS & 0x7f) << 1) | 1);
	
	    int sizeLeft = 188 - pidx;
	    int copy = accessUnit.length;
	    if (copy > sizeLeft) {
	        copy = sizeLeft;
	    }
	
	    // Copy payload
	    System.arraycopy(accessUnit,0,buffer,pidx,copy);

	    output(buffer);
	    mNumTSPacketsWritten++;
	
	    offset = copy;
	    while (offset < accessUnit.length) {
	        lastAccessUnit = ((accessUnit.length - offset) < 184);
	        // for subsequent fragments of "buffer":
	        // 0x47
	        // transport_error_indicator = b0
	        // payload_unit_start_indicator = b0
	        // transport_priority = b0
	        // PID = b0 0001 1110 ???? (13 bits) [0x01e0 + 1 + sourceIndex]
	        // transport_scrambling_control = b00
	        // adaptation_field_control = b??
	        // continuity_counter = b????
	        // the fragment of "buffer" follows.
	
		    for (int i=0; i<188; i++)
		    	buffer[i] = (byte)0xff;
	
		    continuity_counter = ArnoVideoSourceIncrementContinuityCounter();
	
	        pidx = 0;
	        buffer[pidx++] = 0x47;
	        buffer[pidx++] = (byte)(0x00 | (byte)(PID >> 8));
	        buffer[pidx++] = (byte)(PID & 0xff);
	        buffer[pidx++] = (byte)((adaptation ? 0x30 : 0x10) | continuity_counter);
	
	        if (adaptation) {
	        	
	        	int paddingSize = 0;
	        	if (lastAccessUnit)
	        		paddingSize = 184 - (accessUnit.length - offset);
	        	else
	        		paddingSize = 8; // Arno PCR
	            buffer[pidx++] = (byte)(paddingSize - 1); // Adaptation length
	        	buffer[pidx++] = 0x10; // Adaptation bits: set PCR_flag
	            buffer[pidx++] = (byte)(( PCR >> 34 ) & 0xff);
	            buffer[pidx++] = (byte)(( PCR >> 26 ) & 0xff);
	            buffer[pidx++] = (byte)(( PCR >> 18 ) & 0xff);
	            buffer[pidx++] = (byte)(( PCR >> 10 ) & 0xff);
	            buffer[pidx++] = (byte)(0x7e | ( ( PCR & (1<<9) ) >> 2 ) | ( ( PCR & (1<<8) ) >> 8 ));
	            buffer[pidx++] = (byte)(PCR & 0xff);
	            
	        	if (lastAccessUnit)
	        	{
	        		pidx += paddingSize-8;
	        	}
	        }
	
	        sizeLeft = 188 - pidx;
	        copy = accessUnit.length - offset;
	        if (copy > sizeLeft) {
	            copy = sizeLeft;
	        }

	        // Copy payload
		    System.arraycopy(accessUnit,offset,buffer,pidx,copy);

		    output(buffer);
		    mNumTSPacketsWritten++;

	        offset += copy;
	    }
	}

	
	
	public void writeTS() throws IOException 
	{
	    if (mNumTSPacketsWritten >= mNumTSPacketsBeforeMeta) {
	        writeProgramAssociationTable();
	        writeProgramMap();
	
	        mNumTSPacketsBeforeMeta = mNumTSPacketsWritten + 1280; // 2500
	    }
	}

	public void initCrcTable() {
	    int poly = 0x04C11DB7;
	
	    for (int i = 0; i < 256; i++) {
	        int crc = i << 24;
	        for (int j = 0; j < 8; j++) {
	            crc = (crc << 1) ^ (((crc & 0x80000000) != 0) ? (poly) : 0);
	        }
	        mCrcTable[i] = crc;
	    }
	}
	
	/**
	 * Compute CRC32 checksum for buffer starting at offset start and for length
	 * bytes.
	 */
	public byte[] crc32(byte[] data, int offset, int length) {
	    int crc = 0xFFFFFFFF;
	
	    for (int i=offset; i<offset+length; i++) {
	        crc = (crc << 8) ^ mCrcTable[((crc >> 24) ^ data[i]) & 0xFF];
	    }
	
		byte[] res = new byte[4];
		res[0] = (byte)(crc >>> 24);
		res[1] = (byte)(crc >>> 16);
		res[2] = (byte)(crc >>> 8);
		res[3] = (byte)(crc & 0xff);
		return res;
	}
	
	
	/*
	 * Interface between H264Packetizer and NativeLib
	 */
	
	public void output(byte[] mpegtsunit)
	{
		try
		{
			// Arno: with MPEGTS units are 188 bytes. To avoid many calls/second
			// to swift::LiveAdd we cache the data here until limit reached
			mMpegtsBAOS.write(mpegtsunit);
			if (mMpegtsBAOS.size() > 2048)
			{
				byte[] mpegtsblock = mMpegtsBAOS.toByteArray(); 
				
				// LiveAdd waits till it has chunk_size bytes, so writing < 1024
				// here OK.
				mNativeLib.LiveAdd(mSwarmID,mpegtsblock,0,mpegtsblock.length);
				
				mMpegtsBAOS.reset();
			}
		}
		catch(IOException e)
		{
			Log.w("Swift","IOException in MPEG2TSWriter::output");
		}
	}
	
	
	public void input(byte[] h264nalu, int offset, int length, long rtpTimestamp)
	{
		try
		{
			writeTS();
			
			ByteArrayOutputStream h264boas = new ByteArrayOutputStream();
			
			// TODO: optimize, don't write on each NALU
			h264boas.write(mH264AccessUnitDelimiter);
			h264boas.write(mH264SequenceParamSet);
			h264boas.write(mH264PictureParamSet);
			
			h264boas.write(h264nalu,offset,length);
			byte[] h264data = h264boas.toByteArray();
					
			writeAccessUnit(h264data, mTimeStamp);
			//writeAccessUnit(h264data, PTSaheadOfPCRus + rtpTimestamp*10000);
			mTimeStamp += mTimeStampIncr;
		}
		catch(IOException e)
		{
			Log.w("Swift","IOException in MPEG2TSWriter::input");
		}
	}
} 

