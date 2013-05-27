package coop.jcfoodcoop.productupdate

import java.util

/**
 * @author akrieg
 */
class SKnowledgeBase {
    val kbMap = new util.TreeMap[String, SProductEntry]()

    def addEntry(entry:SProductEntry) = {
        kbMap.put(createKey(entry), entry)

    }

    def lookup(product:SProductEntry) = {
        kbMap.get(createKey(product))
    }

    def remove(product:SProductEntry) :SProductEntry ={
        kbMap.remove(createKey(product))
    }


    def createKey(entry:SProductEntry) :String =  {
        if (entry.manufacturer != null) {
            return "${entry.manufacturer}::${entry.productDescription}"
        }
        "::${entry.productDescription}"
    }

    def entries() = {
        kbMap.values()
    }



}

