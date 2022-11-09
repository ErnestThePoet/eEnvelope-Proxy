package utils;

import java.util.List;

public class ByteArrayUtil {
    public static byte[] concat(byte[]...byteArrays){
        int totalLength=0;
        for(var i:byteArrays){
            totalLength+=i.length;
        }

        byte[] concatenated=new byte[totalLength];

        int currentCopyStartIndex=0;

        for(var i:byteArrays){
            System.arraycopy(i,0,concatenated,currentCopyStartIndex,i.length);
            currentCopyStartIndex+=i.length;
        }

        return concatenated;
    }

    public static byte[] concat(List<byte[]> byteArrays){
        int totalLength=byteArrays.stream().reduce(
                0,
                (c,b)->c+b.length,
                (a,b)->b
        );

        byte[] concatenated=new byte[totalLength];

        int currentCopyStartIndex=0;

        for(var i:byteArrays){
            System.arraycopy(i,0,concatenated,currentCopyStartIndex,i.length);
            currentCopyStartIndex+=i.length;
        }

        return concatenated;
    }

    public static boolean equals(byte[] a,byte[] b){
        if(a==null||b==null){
            return false;
        }

        if(a.length!=b.length){
            return false;
        }

        for(int i=0;i<a.length;i++){
            if(a[i]!=b[i]){
                return false;
            }
        }

        return true;
    }

    public static byte[] getByteArrayFromInt32(int value) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) (value & 0xFF);
        byteArray[1] = (byte) ((value & 0xFF00) >>> 8);
        byteArray[2] = (byte) ((value & 0xFF0000) >>> 16);
        byteArray[3] = (byte) ((value & 0xFF000000) >>> 24);
        return byteArray;
    }

    public static int getInt32FromByteArray(byte[] data) {
        int value = ((int) data[0]) & 0xFF;
        value |= (((int) data[1]) & 0xFF) << 8;
        value |= (((int) data[2]) & 0xFF) << 16;
        value |= (((int) data[3]) & 0xFF) << 24;
        return value;
    }
}
