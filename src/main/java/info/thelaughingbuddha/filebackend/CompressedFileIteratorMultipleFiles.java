package info.thelaughingbuddha.filebackend;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CompressedFileIteratorMultipleFiles implements Iterator<IOQContent>, AutoCloseable {

    private final Iterator<Path> pathsIterator;

    private final CompressedFileIteratorSingleFile ioqContentsIterator;

    private IOQContent nextIOQContent;

    CompressedFileIteratorMultipleFiles(Collection<Path> paths) {
        pathsIterator = paths.iterator();
        ioqContentsIterator = new CompressedFileIteratorSingleFile();
    }


    @Override
    public void close() throws Exception {
        ioqContentsIterator.close();
    }

    @Override
    public boolean hasNext() {
        if (nextIOQContent != null) {
            return false;
        }
        while (!ioqContentsIterator.hasNext()) {
            if (!pathsIterator.hasNext()) {
                return false;
            }
            Path path = pathsIterator.next();
            ioqContentsIterator.init(path);
        }
        nextIOQContent = ioqContentsIterator.next();
        return true;
    }

    @Override
    public IOQContent next() {
        if (nextIOQContent != null || hasNext()) {
            IOQContent result = nextIOQContent;
            nextIOQContent = null;
            return result;
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        Iterator.super.remove();
    }
}
