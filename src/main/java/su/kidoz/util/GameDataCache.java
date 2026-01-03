package su.kidoz.util;

public interface GameDataCache {
    byte[] get(int index);

    int add(byte[] data);

    int indexOf(byte[] data);

    int size();

    boolean isEmpty();

    void clear();

    boolean contains(byte[] data);

    byte[] set(int index, byte[] data);

    byte[] remove(int index);
}
