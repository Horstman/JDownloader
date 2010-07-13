package jd.plugins.optional.neembuu;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import jd.plugins.DownloadLink;
import jpfm.DirectoryStream;
import jpfm.FileFlags;
import jpfm.JPfmError;
import jpfm.annotations.MightBeBlocking;
import jpfm.annotations.NonBlocking;
import jpfm.operations.readwrite.ReadRequest;
import jpfm.volume.BasicAbstractFile;
import jpfm.volume.CommonFileAttributesProvider;

/**
 * 
 * @author Shashank Tulsyan
 * @author coalado
 */
public class JDFile extends BasicAbstractFile {
    private DownloadLink downloadLink;

    private final ConcurrentLinkedQueue<ReadRequest> pendingReadRequests = new ConcurrentLinkedQueue<ReadRequest>();
    private final Object lock = new Object();
    private ReadThread readThread = null;
    // an example implementation
    private AsynchronousFileChannel fileChannel;

    public JDFile(DownloadLink downloadLink, DirectoryStream parent) {
        this(downloadLink, parent, CommonFileAttributesProvider.DEFAULT
        /* contains junk values for creation date and other similar fields */);
        // Object of CommonFileAttributesProvider contains
        // info like creation date, can have junk values and
        // can be changed during runtime
        try {
            // fileChannel = AsynchronousFileChannel.open(
            // downloadLink.getDownloadInstance().getFile().toPath());
        } catch (Exception ioe) {

        }
    }

    public JDFile(DownloadLink downloadLink, DirectoryStream parent,// the
                                                                    // parent
                                                                    // virtual
                                                                    // folder
            CommonFileAttributesProvider fileAttributesProvider) {
        super(downloadLink.getFinalFileName(), downloadLink.getDownloadSize(), // assuming
                                                                               // this
                                                                               // means
                                                                               // size
                                                                               // of
                                                                               // total
                                                                               // file
                parent, fileAttributesProvider);
        this.downloadLink = downloadLink;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    public void open() {
        // called when the file is doubled clicked and opened
        // in something like vlc.
        // this is called again only if all instances are closed
        // and file is opened again

        // In Neembuu 's demo implementation
        // We start downloading only when the file is opened
        // In JDownloader, we could have different download
        // modes. #1) Download goes on does not care if virtual file is open or
        // not
        // #2) Download stictly follows request. That is starts when the
        // file is opened, stops when it is closed.
        // @see #close()

        // start read dispatcher when file opened, and stop the
        // thread when file closed.
        readThread = new ReadThread(super.getName());
        this.readThread.start();
    }

    public int read(long offset, ByteBuffer directByteBuffer) {
        // forget this, this is just for possible improved compatibility.

        throw new UnsupportedOperationException("Not supported yet.");
    }

    @NonBlocking
    public void read(ReadRequest read) throws Exception {
        // we want that read requests are handled in a seperate thread.
        // Because if we block in this thread, reading of ALL files
        // in the volume will also be blocked. That is,
        // this single thread that we are in, dispatches read requests
        // on all files in the volume. And if we block the execution of
        // the thread in this function, other read requests will just not
        // be able to dispatch.

        // that is request dispatched in other thread
        // goto #readImpl(ReadRequest read)
        pendingReadRequests.add(read);
        synchronized (lock) {
            lock.notifyAll();
        }

    }

    // /////////////////////////////////////////
    // /////////////////////////////////////////
    // ///////////This is the most//////////////
    // ////////////////important////////////////
    // ////////////////function.////////////////
    // //////////Should be implemented//////////
    // /////////////as non-blocking/////////////
    // /////////////////////////////////////////
    @MightBeBlocking(reason = "Nature of this function depends on how jd implements support for it.")
    // This function should be non-blocking, right hand clicking the file in
    // explorer
    // will surely make explorer go in NOT RESPONDING state. One solution could
    // be
    // to implement in non-bloking fashion, other could be to dispatch each and
    // every arequest in
    // a separate thread. // todo ^^^ remove all this
    private void readImpl(ReadRequest read) {

        // if data present read from saved file
        // otherwise make sure that region is downloaded.
        // do not wait while that region is being downloaded
        // somehow do this in non-blocking fashion
        read.complete(JPfmError.ACCESS_DENIED, 0, null);
        // fileChannel.read(read.getByteBuffer(), read.getFileOffset(), read,
        // jpfm.util.ReadCompletionHandler.INSTANCE );
    }

    public synchronized void close() {
        // called when each and every instance of file is closed.
        if (this.readThread != null) {
            while (this.readThread.isAlive()) {
                synchronized (lock) {
                    lock.notifyAll(); // << imp.
                }
            }
        }
        // ???
        // downloadLink.getDownloadLinkController().abortDownload();
    }

    @Override
    public FileFlags getFileFlags() {
        // super.getFileFlags() is fake and useless

        // if the file is executable
        // setExecutable should also be invoked
        // Having executable files in virtual folder is dangerous
        // as the executable can be a malicious program, and
        // antivirus programs are not able to heal the files
        // since these are readonly in the virtual folder
        // Also I don 't think any user will do a watch as you download
        // on an executable.

        return new FileFlags.Builder().setOffline()// these 2 flags means
                .setNoIndex()// that thumnail rendering by explorer/nautilus is
                             // disabled :)
                // these flags also result in a small cross appearing on the
                // file.
                .setReadOnly()// this does not make any difference as we are
                // already in read only filesystem
                .build();
    }

    public void setVirtualFileSize(long newFileSize) {
        synchronized (this) {
            super.fileSize = newFileSize;
            super.setInvalid(); // if the file was open now
            // it will not be readable any more.
            // the file will have to re-opened in the media player.
            // Also, if the file was cached in a HashMap against
            // (this is the case in writable virtual filesystem )
            // fileid, then the old entry should be removed.
        }
    }

    private final class ReadThread extends Thread {
        public ReadThread(String filename) {
            super("ReadDispatcher@" + filename);
        }

        @Override
        public void run() {
            for (; isOpen();) { // service till this file is open
                // a new thread is created everytime all instance of
                // the file are closed and atleast one instance opened.
                Iterator<ReadRequest> it = pendingReadRequests.iterator();
                while (it.hasNext()) {
                    ReadRequest read = it.next();
                    if (read.isCompleted()) {
                        it.remove();
                        continue;
                    }
                    try {
                        readImpl(read);
                        // object has been send once, it can be removed now
                        // we send requests only once.
                        // other AlreadyCompleteException will be certainly
                        // thrown
                        it.remove();
                        continue;
                    } catch (Exception any) {
                        // send error message to logger
                        any.printStackTrace();
                        if (!read.isCompleted()) read.handleUnexpectedCompletion(any);
                    }
                }

                synchronized (lock) {
                    try {
                        // wait while we don't have pending requets and the file
                        // is open
                        while (pendingReadRequests.size() == 0 && isOpen())
                            lock.wait();
                    } catch (InterruptedException exception) {

                    }
                }
            }
        }

    }

}
