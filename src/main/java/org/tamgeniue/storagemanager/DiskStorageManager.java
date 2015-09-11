// Spatial Index Library
//
// Copyright (C) 2002  Navel Ltd.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Contact information:
//  Mailing address:
//    Marios Hadjieleftheriou
//    University of California, Riverside
//    Department of Computer Science
//    Surge Building, Room 310
//    Riverside, CA 92521
//
//  Email:
//    marioh@cs.ucr.edu

package org.tamgeniue.storagemanager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

@Component("diskStorageManager")
public class DiskStorageManager implements IStorageManager, Serializable {

    private static final long serialVersionUID = -3332658777155967375L;
    private RandomAccessFile randomDataFile = null;
    private RandomAccessFile randomIndexFile = null;
    private int pageSize = 0;
    private int mNextPage = -1;
    private TreeSet<Integer> m_emptyPages = new TreeSet<>();
    private HashMap<Integer, PageEntry> m_pageIndex = new HashMap<>();
    private byte[] m_buffer = null;

    private int IO = 0;

    public DiskStorageManager()throws SecurityException, NullPointerException, IOException, IllegalArgumentException {
     //   this(true, "GridDiskBuffer.grid", 4096000);
    }

    public DiskStorageManager(@Value("${config.storage.overwrite}") boolean overwrite,
                              @Value("${config.storage.gridFile}") String fileName,
                              @Value("${config.storage.pageSize}") int pageSize)
            throws SecurityException, NullPointerException, IOException, IllegalArgumentException {

        m_buffer = new byte[this.pageSize];
        File indexFile = new File(fileName + ".idx");
        File dataFile = new File(fileName + ".dat");
        // check if files exist.
        if (!overwrite && (!indexFile.exists() || !dataFile.exists())) overwrite = true;

        if (overwrite) {
            overWrite(indexFile);
            overWrite(dataFile);
        }

        randomIndexFile = new RandomAccessFile(indexFile, "rw");
        randomDataFile = new RandomAccessFile(dataFile, "rw");


        // find page size.
        if (overwrite) {
            this.pageSize = pageSize;
            mNextPage = 0;
        } else {
            this.pageSize = randomIndexFile.readInt();
            mNextPage = randomIndexFile.readInt();

            int count, id, page;
            count = randomIndexFile.readInt();

            for (int cCount = 0; cCount < count; cCount++) {
                page = randomIndexFile.readInt();
                m_emptyPages.add(page);
            }

            // load index table in memory.
            count = randomIndexFile.readInt();

            for (int cCount = 0; cCount < count; cCount++) {
                PageEntry e = new PageEntry();

                id = randomIndexFile.readInt();
                e.setLength(randomIndexFile.readInt());

                int count2 = randomIndexFile.readInt();

                for (int cCount2 = 0; cCount2 < count2; cCount2++) {
                    page = randomIndexFile.readInt();
                    e.getPages().add(page);
                }
                m_pageIndex.put(id, e);
            }
        }
    }

    private void overWrite(File indexFile) throws IOException {
        boolean success = false;
        if (indexFile.exists()) {
            success = indexFile.delete();
        }
        if (!success) throw new IOException("index file or data file deletion failed.");

        boolean b = indexFile.createNewFile();
        if (!b) throw new IOException("file cannot be opened.");
    }

    public void flush() {
        try {
            randomIndexFile.seek(0l);

            randomIndexFile.writeInt(pageSize);
            randomIndexFile.writeInt(mNextPage);

            int count = m_emptyPages.size();
            randomIndexFile.writeInt(count);

            for (Integer m_emptyPage : m_emptyPages) {

                randomIndexFile.writeInt(m_emptyPage);
            }

            count = m_pageIndex.size();
            randomIndexFile.writeInt(count);

            for (Map.Entry<Integer, PageEntry> me : m_pageIndex.entrySet()) {
                int id = me.getKey();
                randomIndexFile.writeInt(id);

                PageEntry e = me.getValue();
                count = e.getPages().size();
                randomIndexFile.writeInt(count);

                for (int cIndex = 0; cIndex < count; cIndex++) {
                    randomIndexFile.writeInt(e.getPages().get(cIndex));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Corrupted index file.");
        }
    }

    public byte[] loadByteArray(final int id) {
        PageEntry e = m_pageIndex.get(new Integer(id));
        if (e == null) throw new InvalidPageException(id);

        int cNext = 0;
        int cTotal = e.getPages().size();

        byte[] data = new byte[e.getLength()];
        int cIndex = 0;
        int cLen;
        int cRem = e.getLength();

        do {
            try {
                randomDataFile.seek(e.getPages().get(cNext) * pageSize);
                int bytesRead = randomDataFile.read(m_buffer);
                if (bytesRead != pageSize) throw new IllegalStateException("Corrupted data file.");
            } catch (IOException ex) {
                throw new IllegalStateException("Corrupted data file.");
            }

            cLen = (cRem > pageSize) ? pageSize : cRem;
            System.arraycopy(m_buffer, 0, data, cIndex, cLen);

            cIndex += cLen;
            cRem -= cLen;
            cNext++;
        }
        while (cNext < cTotal);

        IO++;

        return data;
    }

    public int storeByteArray(final int id, final byte[] data) {
        if (id == NewPage) {
            PageEntry e = new PageEntry();
            e.setLength(data.length);

            int cIndex = 0;
            int cPage;
            int cRem = data.length;
            int cLen;

            while (cRem > 0) {
                if (!m_emptyPages.isEmpty()) {
                    Integer i = m_emptyPages.first();
                    m_emptyPages.remove(i);
                    cPage = i;
                } else {
                    cPage = mNextPage;
                    mNextPage++;
                }

                cLen = (cRem > pageSize) ? pageSize : cRem;
                System.arraycopy(data, cIndex, m_buffer, 0, cLen);

                try {
                    randomDataFile.seek(cPage * pageSize);
                    randomDataFile.write(m_buffer);
                } catch (IOException ex) {
                    throw new IllegalStateException("Corrupted data file.");
                }

                cIndex += cLen;
                cRem -= cLen;
                e.getPages().add(cPage);
            }

            Integer i = e.getPages().get(0);
            m_pageIndex.put(i, e);

            return i;
        } else {
            // find the entry.
            PageEntry oldEntry = m_pageIndex.get(new Integer(id));
            if (oldEntry == null) throw new InvalidPageException(id);

            m_pageIndex.remove(new Integer(id));

            PageEntry e = new PageEntry();
            e.setLength(data.length);

            int cIndex = 0;
            int cPage;
            int cRem = data.length;
            int cLen, cNext = 0;

            while (cRem > 0) {
                if (cNext < oldEntry.getPages().size()) {
                    cPage = oldEntry.getPages().get(cNext);
                    cNext++;
                } else if (!m_emptyPages.isEmpty()) {
                    Integer i = m_emptyPages.first();
                    m_emptyPages.remove(i);
                    cPage = i;
                } else {
                    cPage = mNextPage;
                    mNextPage++;
                }

                cLen = (cRem > pageSize) ? pageSize : cRem;
                System.arraycopy(data, cIndex, m_buffer, 0, cLen);

                try {
                    randomDataFile.seek(cPage * pageSize);
                    randomDataFile.write(m_buffer);
                } catch (IOException ex) {
                    throw new IllegalStateException("Corrupted data file.");
                }

                cIndex += cLen;
                cRem -= cLen;
                e.getPages().add(cPage);
            }

            while (cNext < oldEntry.getPages().size()) {
                m_emptyPages.add(oldEntry.getPages().get(cNext));
                cNext++;
            }

            Integer i = e.getPages().get(0);
            m_pageIndex.put(i, e);

            return i;
        }
    }

    public void deleteByteArray(final int id) {
        // find the entry.
        PageEntry e = m_pageIndex.get(new Integer(id));
        if (e == null) throw new InvalidPageException(id);

        m_pageIndex.remove(new Integer(id));

        for (int cIndex = 0; cIndex < e.getPages().size(); cIndex++) {
            m_emptyPages.add(e.getPages().get(cIndex));
        }
    }

    public int getIO() {
        return IO;
    }
}
