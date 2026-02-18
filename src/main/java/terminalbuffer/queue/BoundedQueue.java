package terminalbuffer.queue;

/**
 * A fixed-capacity FIFO queue implemented as a circular buffer (ring buffer).
 *
 * <p>This queue maintains a fixed capacity and provides O(1) operations for
 * push, pop, and indexed access. When the queue is full, attempting to push
 * will throw an exception rather than automatically evicting elements.
 *
 * <p>The circular buffer implementation avoids shifting elements on removal,
 * making scroll operations in terminal emulation extremely efficient.
 *
 * <p>This class is not thread-safe. External synchronization is required if
 * accessed from multiple threads.
 *
 * @param <E> the type of elements held in this queue
 */
public class BoundedQueue<E> {
    private Object[] list;
    private int capacity;
    private int count;
    private int head, tail;

    /**
     * Creates a bounded queue with the specified capacity.
     *
     * @param capacity maximum number of elements the queue can hold (must be &gt; 0)
     * @throws IllegalArgumentException if capacity is negative or zero
     */
    public BoundedQueue(int capacity){
        list = new Object[capacity];
        this.capacity = capacity;
        count = 0;
        head = 0;
        tail = 0;
    }

    /**
     * Returns the current number of elements in the queue.
     *
     * @return number of elements (0 to capacity)
     */
    public int size(){
        return count;
    }

    /**
     * Retrieves the element at the specified index without removing it.
     *
     * <p>Index 0 refers to the oldest element (front of queue), and
     * index {@code size()-1} refers to the newest element (back of queue).
     *
     * <p>This operation is O(1) due to the circular buffer implementation.
     *
     * @param i index of element to retrieve (0-based)
     * @return element at the specified index
     * @throws IndexOutOfBoundsException if index is negative or &gt;= size()
     */
    public E get(int i) {
        if (i >= size() || i < 0) {
            throw new IndexOutOfBoundsException();
        }
        int index = head + i;
        if (index >= capacity) index -= capacity;
        return (E)list[index];
    }

    /**
     * Removes and returns the oldest element from the queue.
     *
     * <p>This operation is O(1) and does not require shifting elements.
     *
     * @return the oldest element (front of queue)
     * @throws IndexOutOfBoundsException if the queue is empty
     */
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

    /**
     * Adds an element to the back of the queue.
     *
     * <p>This operation is O(1) and does not require shifting elements.
     *
     * @param elem element to add (null elements are allowed)
     * @throws IndexOutOfBoundsException if the queue is at capacity
     */
    public void push(E elem){
        if(size() == capacity){
            throw new IndexOutOfBoundsException();
        }
        list[tail] = elem;
        tail = (tail + 1) % capacity;
        count++;
    }

    /**
     * Checks whether the queue is empty.
     *
     * @return true if the queue contains no elements, false otherwise
     */
    public boolean empty(){
        return size() == 0;
    }

    /**
     * Removes all elements from the queue.
     *
     * <p>After this operation, {@link #size()} returns 0 and {@link #empty()}
     * returns true. The capacity remains unchanged.
     */
    public void clear(){
        for (int i = 0; i < capacity; i++){
            list[i] = null;
        }
        count = 0;
        head = 0;
        tail = 0;
    }

    /**
     * Resizes the queue to a new capacity and clears all existing elements.
     *
     * <p>This operation is used during terminal resize operations to adjust
     * the screen buffer capacity. All existing elements are discarded.
     *
     * <p>After this operation:
     * <ul>
     *   <li>The queue is empty ({@link #size()} returns 0)</li>
     *   <li>The capacity is set to {@code newCapacity}</li>
     *   <li>The internal pointers are reset</li>
     * </ul>
     *
     * @param newCapacity new maximum number of elements (must be &gt; 0)
     */
    public void resizeAndClear(int newCapacity){
        list = new Object[newCapacity];
        capacity = newCapacity;
        clear();
    }
}