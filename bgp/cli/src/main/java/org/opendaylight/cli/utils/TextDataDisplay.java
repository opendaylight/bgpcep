/* TextDataDisplay.java

* Created on May 2, 2007, 1:31 PM / dfuchs
*
* SCCS: %W%
*
* Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
*   - Redistributions of source code must retain the above copyright
*     notice, this list of conditions and the following disclaimer.
*
*   - Redistributions in binary form must reproduce the above copyright
*     notice, this list of conditions and the following disclaimer in the
*     documentation and/or other materials provided with the distribution.
*
*   - Neither the name of Sun Microsystems nor the names of its
*     contributors may be used to endorse or promote products derived
*     from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
* IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
* THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
* PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.opendaylight.cli.utils;


import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;


public class TextDataDisplay {

    //A logger for this class.
    private static final Logger LOG =
            Logger.getLogger(TextDataDisplay.class.getName());

    //Creates a new instance of TextDataDisplay
    public TextDataDisplay() {
    }

    public String display(String name, Object data) {
        return display("",name,data);
    }

    public String display(String prefix, String name, Object data) {
        if (name == null)
            throw new IllegalArgumentException("name must not be null");
        final StringBuffer buffer = new StringBuffer();
        return write(buffer,"",name,data).toString();
    }

    public StringBuffer write(StringBuffer buffer, String prefix,
                              String name, Object data) {
        if (data == null) return writeSimple(buffer,prefix,name,null,true);
        if (data.getClass().isArray())
            return writeArray(buffer,prefix,name,data);
        if (data instanceof CompositeData)
            return writeCompositeData(buffer,prefix,name,(CompositeData)data);
        if (data instanceof TabularData)
            return writeTabularData(buffer,prefix,name,(TabularData)data);
        if (data instanceof Map)
            return writeMap(buffer,prefix,name,(Map)data);
        if (data instanceof Collection) {
            return writeArray(buffer,prefix,name,((Collection)data).toArray());
        }
        return writeSimple(buffer,prefix,name,data,true);
    }

    String toString(Object data) {
        if (data==null) return "null";
        else return data.toString();
    }

    StringBuffer writeSimple(StringBuffer buffer, String prefix,
                             String name, Object data, boolean writeline) {
        buffer.append(prefix).append(name).append("=").append(toString(data));
        if (writeline) buffer.append("\\n");
        return buffer;
    }

    StringBuffer writeArray(StringBuffer buffer, String prefix,
                            String name, Object array) {
        if (array == null)
            return writeSimple(buffer,prefix,name,null,true);
        final int length = Array.getLength(array);
        for (int i=0;i<length;i++) {
            final Object data = Array.get(array,i);
            write(buffer,prefix,name+"["+i+"]",data);
        }
        return buffer;
    }

    StringBuffer writeCompositeData(StringBuffer buffer,
                                    String prefix, String name, CompositeData data) {
        if (data == null)
            return writeSimple(buffer,prefix,name,null,true);
        writeSimple(buffer,prefix,name,"CompositeData("+
                data.getCompositeType().getTypeName()+")",true);
        buffer.append(prefix).append("{").append("\\n");
        final String fieldprefix = prefix + " ";
        for (String key : data.getCompositeType().keySet()) {
            write(buffer,fieldprefix,name+"."+key,data.get(key));
        }
        buffer.append(prefix).append("}").append("\\n");
        return buffer;
    }

    StringBuffer writeTabularData(StringBuffer buffer,
                                  String prefix, String name, TabularData data) {
        if (data == null)
            return writeSimple(buffer,prefix,name,null,true);
        writeSimple(buffer,prefix,name,"TabularData("+
                data.getTabularType().getTypeName()+")",true);
        final List<String> keyNames = data.getTabularType().getIndexNames();
        final int indexCount = keyNames.size();
        for (Object keys : data.keySet()) {
            final Object[] keyValues = ((List<?>)keys).toArray();
            final StringBuilder b = new StringBuilder(name);
            b.append("[");
            for (int i=0;i<indexCount;i++) {
                if (i>0) b.append(", ");
                b.append(keyNames.get(i)+"="+keyValues[i]);
            }
            b.append("]");
            writeCompositeData(buffer,prefix,b.toString(),data.get(keyValues));
            b.append("\\n");
        }
        return buffer;
    }

    StringBuffer writeMap(StringBuffer buffer,
                          String prefix, String name, Map<Object,Object> data) {
        if (data == null)
            return writeSimple(buffer,prefix,name,null,true);
        writeSimple(buffer,prefix,name,"java.util.Map",true);
        for (Entry<Object,Object> e : data.entrySet()) {
            write(buffer,prefix,name+"["+e.getKey()+"]",e.getValue());
        }
        return buffer;
    }

}