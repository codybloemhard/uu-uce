package com.uu_uce.misc

class LinkedListIterator<T>(private var cur: Node<T>?): Iterator<Node<T>>{
    val first = cur
    private var firstTime = true
    override fun hasNext(): Boolean {
        if(cur == first){
            if(firstTime) firstTime = false
            else return false
        }
        return cur != null
    }
    override fun next(): Node<T> {
        val res = cur ?: throw Exception("next called while remaining collection is empty")
        cur = cur?.next
        return res
    }
}

class Node<T>(val value: T){
    var prev: Node<T>? = null
    var next: Node<T>? = null
}

//not available in Kotlin so made a quick version myself
class LinkedList<T>(e: List<T>, private val cyclic: Boolean = false): Iterable<Node<T>> {
    var first: Node<T>? = null
    private var last: Node<T>? = null
    var size = 0
    init{
        size = e.size
        var prev: Node<T>? = null
        var cur: Node<T>? = null
        for(item in e){
            prev = cur
            cur = Node(item)
            if(prev == null) first = cur
            else {
                prev.next = cur
                cur.prev = prev
            }
        }
        last = cur

        if(cyclic){
            first?.prev=last
            last?.next=first
        }
    }

    fun add(element: T){
        val newNode = Node(element)
        if(first == null) {
            size++
            first = newNode
            last = newNode
            if(cyclic){
                first?.next = last
                first?.prev = last
                last?.next = first
                last?.prev = first
            }
        }
        else {
            addAfter(element,last)
        }
    }

    private fun addAfter(element: T, after: Node<T>?){
        size++
        val newNode = Node(element)
        after?.next?.prev = newNode
        newNode.next = after?.next
        after?.next = newNode
        newNode.prev = after
    }

    fun remove(element: Node<T>){
        size--
        element.prev?.next = element.next
        element.next?.prev = element.prev
        if(element == first) first = element.next
        if(element == last) last = element.prev
    }

    override fun iterator(): Iterator<Node<T>> {
        return LinkedListIterator(first)
    }
}