package models.blog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import siena.Column;
import siena.Generator;
import siena.Id;
import siena.Json;
import siena.Model;
import siena.Query;

public class Post extends Model {

    @Id(Generator.AUTO_INCREMENT)
    public Long id;

    @Column("title")
    public String title;

    @Column("date")
    public Date date;

    @Column("initialDate")
    public Date initialDate;

    @Column("content")
    public String content;

    @Column("published")
    public boolean published;

    @Column("comments")
    private List<Long> commentIdList;

    @Column("tags")
    public List<String> tags;

    public Post() {
        date = new Date();
        initialDate = new Date();
        published = true;
    }

    public Post(String title, String content) {
        this();
        this.title = title;
        this.content = content;
    }

    @Deprecated
    public static Query<Post> all() {
        return Model.all(Post.class);
    }

    /**
     * Return a post by id.
     * 
     * @param id
     * @return the post, null if not found
     */
    public static Post findById(long id) {
        return Post.all().filter("id", id).get();
    }

    public static void delete(long id) {
        Post post = Post.findById(id);
        if (post != null) {
            if (post.commentIdList != null) {
                for (long commentId : post.commentIdList) {
                    Comment.delete(commentId);
                }
            }
            post.delete();
        }
    }

    /**
     * Add a comment to this post. Save the comment and update the post.
     * 
     * @param comment
     */
    public void addComment(Comment comment) {
        if (commentIdList == null) {
            commentIdList = new ArrayList<Long>();
        }

        comment.insert();
        commentIdList.add(comment.id);
        update();
    }

    public void deleteComment(long id) {
        if (commentIdList == null) {
            return;
        }
        for (long commentId : commentIdList) {
            if (commentId == id) {
                Comment.delete(commentId);
                commentIdList.remove(new Long(id));
                update();
                return;
            }
        }
    }

    public int getCommentCount() {
        if (commentIdList != null) {
            return commentIdList.size();
        } else {
            return 0;
        }
    }

    public List<Comment> getComments() {
        List<Comment> comments = new ArrayList<Comment>();
        if (commentIdList != null) {
            for (long commentId : commentIdList) {
                Comment comment = Comment.findById(commentId);
                if (comment != null) {
                    comments.add(comment);
                }
            }
        }
        return comments;
    }

    public void setTags(String tags) {
        String[] tagArray = tags.split(",");
        this.tags = new ArrayList();
        for (String tag : tagArray) {
            String t = tag.trim();
            if (!t.isEmpty()) {
                this.tags.add(t);
                Tag.use(t);
            }
        }
    }

    public String getTagString() {
        if (tags == null) {
            return "";
        }
        StringBuilder tagString = new StringBuilder();
        for (String tag : tags) {
            tagString.append(tag).append(", ");
        }
        return tagString.toString();
    }

    @Override
    public String toString() {
        Json[] comments = new Json[getCommentCount()];
        int index = 0;
        for (Comment comment : getComments()) {
            comments[index++] = comment.toJson();
        }
        Json json = Json.map().put("id", id).put("title", title).put("date", date.getTime()).put("content", content)
                .put("published", published).put("tags", tags).put("comments", comments);
        return json.toString();
    }
}