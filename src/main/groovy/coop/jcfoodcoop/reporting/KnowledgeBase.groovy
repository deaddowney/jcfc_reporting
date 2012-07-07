package coop.jcfoodcoop.reporting

/**
 * @author akrieg
 */
class KnowledgeBase {
    NavigableMap<String, ProductEntry> kbMap = new TreeMap<String, ProductEntry>();
    
    public void addEntry(ProductEntry entry) {
        kbMap.put(createKey(entry), entry)
        
    }

    public ProductEntry lookup(ProductEntry product) {
        return kbMap.get(createKey(product))
    }

    public ProductEntry remove(ProductEntry product) {
        return kbMap.remove(createKey(product))
    }


    String createKey(ProductEntry entry) {
        if (entry.manufacturer != null) {
            return "${entry.manufacturer}::${entry.productDescription}"
        }
        return "::${entry.productDescription}"
    }

    public Collection<ProductEntry> entries() {
        return kbMap.values()
    }



}
