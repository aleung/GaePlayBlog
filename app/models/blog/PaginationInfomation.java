package models.blog;

public class PaginationInfomation {

    public int totalSize;
    public int from;
    public int to;
    public int pageSize;
    public String tag = null;

    public PaginationInfomation(int totalSize, int from, int to, int pageSize) {
        this.totalSize = totalSize;
        this.from = from;
        this.to = to;
        this.pageSize = pageSize;
    }

    public PaginationInfomation(int totalSize, int from, int to, int pageSize, String tag) {
        this(totalSize, from, to, pageSize);
        this.tag = tag;
    }

}
