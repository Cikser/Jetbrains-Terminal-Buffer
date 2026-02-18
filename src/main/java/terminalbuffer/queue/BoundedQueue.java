package terminalbuffer.queue;

public class BoundedQueue<E> {
    private Object[] list;
    private int capacity;
    private int count;
    private int head, tail;

    public BoundedQueue(int capacity){
        list = new Object[capacity];
        this.capacity = capacity;
        count = 0;
        head = 0;
        tail = 0;
    }

    public int size(){
        return count;
    }

    public E get(int i) {
        if (i >= size() || i < 0) {
            throw new IndexOutOfBoundsException();
        }
        int index = head + i;
        if (index >= capacity) index -= capacity;
        return (E)list[index];
    }

    public E pop(){
        if(size() == 0){
            throw new IndexOutOfBoundsException();
        }
        E ret = (E)list[head];
        list[head] = null;
        head = (head + 1) % capacity;
        count--;
        return ret;
    }

    public void push(E elem){
        if(size() == capacity){
            throw new IndexOutOfBoundsException();
        }
        list[tail] = elem;
        tail = (tail + 1) % capacity;
        count++;
    }

    public boolean empty(){
        return size() == 0;
    }

    public void clear(){
        for (int i = 0; i < capacity; i++){
            list[i] = null;
        }
        count = 0;
        head = 0;
        tail = 0;
    }

    public void resizeAndClear(int newCapacity){
        list = new Object[newCapacity];
        capacity = newCapacity;
        clear();
    }
}