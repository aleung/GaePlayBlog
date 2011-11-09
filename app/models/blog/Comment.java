package models.blog;

import java.util.Date;

import siena.Column;
import siena.Generator;
import siena.Id;
import siena.Json;
import siena.Model;
import siena.Query;

public class Comment extends Model {

    @Id(Generator.AUTO_INCREMENT)
    public Long id;

    @Column("date")
    public Date date;

    @Column("content")
    public String content;

    @Column("by")
    public String by;

    @Column("email")
    public String email;

    @Column("link")
    public String link;

    @Column("postId")
    public Long postId;

    @Column("adminReply")
    private String reply;

    @Column("adminReplyDate")
    private Date replyDate;

    private Post post;

    public Comment() {
        date = new Date();
    }

    public Comment(long postId, String by, String email, String link, String content) {
        this();
        this.postId = postId;
        this.by = by;
        this.email = email;
        setLink(link);
        this.content = content;
    }

    public void setLink(String link) {
        if ((link != null) && !link.isEmpty()) {
            if (link.startsWith("http://") || link.startsWith("https://")) {
                this.link = link;
            } else {
                this.link = "http://" + link;
            }
        }
    }

    public static Query<Comment> all() {
        return Model.all(Comment.class);
    }

    /**
     * Return a comment by id.
     * 
     * @param id
     * @return the comment, null if not found
     */
    public static Comment findById(long id) {
        return Comment.all().filter("id", id).get();
    }

    public static void delete(long id) {
        Comment comment = new Comment();
        comment.id = id;
        comment.delete();
    }
    
    public void reply(String content) {
        this.reply = content;
        this.replyDate = new Date();
        update();
    }

    public String getReply() {
        return reply;
    }

    public Post getPost() {
        if (post == null) {
            post = Post.findById(postId);
        }
        return post;
    }

    public Json toJson() {
        Json json = Json.map().put("id", id).put("by", by).put("email", email).put("link", link).put("date",
                date.getTime()).put("content", content).put("postId", postId).put("reply", reply);
        if (replyDate != null) {
            json = json.put("replyDate", replyDate.getTime());
        }
        return json;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
