/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of simpledeb.
 *
 * simpledeb is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * simpledeb is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with simpledeb; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.simpledeb.core.action;

public class HexUtil {

	private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    public static String toHex(byte[] data) {
    	
        StringBuilder r = new StringBuilder(data.length * 2);
        
        for ( byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        
        return r.toString();
    }
    
    public static byte[] fromHex(String hex) {
    	
    	if (hex.length() % 2 != 0) {
    		hex = "0" + hex;
    	}
    	
    	int byteCount = hex.length() / 2;
    	
    	byte[] bytes = new byte[byteCount];
    	
    	for (int i=0; i<byteCount; i++) {
    		int index = i * 2;
    		
    		char upper = hex.charAt(index);
//    		System.out.println("Upper: " + upper);
    		
    		int upperValue = hexToByte(upper) << 4;
//    		System.out.println("Upper Value: " + upperValue);
    		
    		char lower = hex.charAt(index + 1);
//    		System.out.println("Lower: " + lower);
    		
    		int lowerValue = hexToByte(lower);
//    		System.out.println("Lower Value: " + lowerValue);
    		
    		int value = upperValue + lowerValue;
//    		System.out.println("Value: " + value);
    		
    		bytes[i] = (byte)value;
    	}
    	
    	return bytes;
    }
    
    private static int hexToByte(char hex) {
    	
    	int offset;
    	
    	if (hex >= '0' && hex <= '9') {
    		offset = -48;
    	}
    	else if (hex >= 'A' && hex <= 'F') {
    		offset = -55;
    	}
    	else if (hex >= 'a' && hex <= 'f') {
    		offset = -87;
    	}
    	else {
    		throw new RuntimeException("Not a hexidecimal character: " + hex);
    	}
    	
    	int hexValue = (int)hex;
    	return hexValue + offset;
    }
}
