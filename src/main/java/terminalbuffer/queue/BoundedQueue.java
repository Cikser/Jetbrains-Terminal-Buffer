package terminalbuffer.queue;

import java.util.ArrayList;

public class BoundedQueue<E> {
    private final ArrayList<E> list;
    private final int capacity;
    private int count;
    private int head, tail;

    public BoundedQueue(int capacity){
        list = new ArrayList<>(capacity);
        this.capacity = capacity;
        count = 0;
        head = 0;
        tail = 0;
    }

    public int size(){
        return count;
    }

    public E get(int i) {
        if (i >= size()) {
            throw new IndexOutOfBoundsException();
        }
        return list.get((head + i) % capacity);
    }

    public E pop(){
        if(size() == 0){
            throw new IndexOutOfBoundsException();
        }
        E ret = list.get(head);
        head = (head + 1) % capacity;
        count--;
        return ret;
    }

    public void push(E elem){
        if(size() == capacity){
            throw new IndexOutOfBoundsException();
        }
        if(list.size() < capacity){
            list.add(elem);
        }
        else{
            list.set(tail, elem);
        }
        tail = (tail + 1) % capacity;
        count++;
    }

    public boolean empty(){
        return size() == 0;
    }

    public void clear(){
        list.clear();
        count = 0;
        head = 0;
        tail = 0;
    }
}