package controllers;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import models.blog.Comment;
import models.blog.PaginationInfomation;
import models.blog.Post;
import models.blog.Tag;
import notifiers.Mails;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.modules.gae.GAE;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import siena.Query;

public class Blog extends Controller {

    private static final int PAGE_SIZE = 5;
    private static final int FEED_SIZE = 10;
    private static final int COMMENT_PAGE_SIZE = 25;

    @Before
    private static void checkUser() {
        if (GAE.isLoggedIn()) {
            renderArgs.put("loginUser", GAE.getUser().getEmail());
            if (GAE.isAdmin()) {
                renderArgs.put("isAdmin", true);
            }
        }
    }

    public static void showList() {
        showPosts("");
    }

    public static void showListByTag(String tag) {
        showPosts(tag);
    }

    public static void showAtom() {
        Query<Post> query = Post.all();
        List<Post> posts = query.order("-date").fetch(FEED_SIZE);
        Http.Request.current().format = "xml";
        render(posts);
    }

    public static void commentList() {
        int offset = getOffsetParam();
        Query<Comment> query = Comment.all();
        List<Comment> comments = query.order("-date").fetch(COMMENT_PAGE_SIZE, offset);
        PaginationInfomation paginationInfo = new PaginationInfomation(query.count(), offset + 1, offset
                + comments.size(), COMMENT_PAGE_SIZE);
        render(comments, paginationInfo);
    }

    /**
     * Show the posts list
     * 
     * @param tag Filter posts by tag. No filter if it is empty.
     */
    private static void showPosts(String tag) {
        int offset = getOffsetParam();
        Query<Post> query = Post.all();
        if (!tag.isEmpty()) {
            query = query.filter("tags", tag);
        }
        List<Post> posts = query.order("-date").fetch(PAGE_SIZE, offset);
        PaginationInfomation paginationInfo = new PaginationInfomation(query.count(), offset + 1,
                offset + posts.size(), PAGE_SIZE, tag);
        render("Blog/showPosts.html", posts, paginationInfo);
    }

    public static void showPost(long id) {
        Post post = Post.findById(id);
        if (post == null) {
            notFound();
        }
        render(post);
    }

    public static void deletePost(long id) {
        checkAdmin();
        Post.delete(id);
        showList();
    }

    /**
     * Create or update a post.
     * 
     * @param id If id is -1, create new post; otherwise update the post
     * @param title
     * @param content
     * @param tags
     */
    public static void putPost(long id, @Required String title, @Required String content, String tags) {
        checkAdmin();
        if (Validation.hasErrors()) {
            render(title, content, tags, id);
        }

        Post post;
        if (id == -1) {
            post = new Post(title, content);
            post.setTags(tags);
            post.insert();
        } else {
            post = Post.findById(id);
            if (post == null) {
                notFound(String.format("Post %d not exist", id));
            }
            post.title = title;
            post.content = content;
            post.setTags(tags);
            post.date = new Date();
            post.update();
        }

        showPost(post.id);
    }

    /**
     * Show the new post form
     */
    public static void newPostForm() {
        checkAdmin();
        long id = -1;
        render("Blog/putPost.html", id);
    }

    /**
     * Show the edit post form
     */
    public static void editPostForm(long id) {
        checkAdmin();
        Post post = Post.findById(id);
        if (post == null) {
            notFound();
        }
        String title = post.title;
        String content = post.content;
        String tags = post.getTagString();
        render("Blog/putPost.html", id, title, content, tags);
    }

    public static void addComment(long id, String by, String link, @Required String content) {
        if (Validation.hasErrors()) {
            params.flash();
            Validation.keep();
            showPost(id);
        }

        String email = GAE.getUser().getEmail();
        if (by.isEmpty()) {
            by = email;
        }

        Post post = Post.findById(id);
        Comment comment = new Comment(id, by, email, link, content);
        post.addComment(comment);
        showPost(id);
    }

    private static int getOffsetParam() {
        String offset = params.get("offset");
        try {
            int result = Integer.parseInt(offset);
            if (result < 0) {
                result = 0;
            }
            return result;
        } catch (Exception e) {
            return 0;
        }
    }

    private static void checkAdmin() {
        if (!GAE.isAdmin()) {
            forbidden();
        }
    }

    public static void importForm() {
        checkAdmin();
        Logger.debug("show import form");
        render();
    }

    public static void importBlogbus(@Required String xml) throws Exception {
        checkAdmin();
        if (Validation.hasErrors()) {
            render("Blog/importForm.html");
        }

        SAXReader reader = new SAXReader();
        Document document = reader.read(new StringReader(xml));
        Element root = document.getRootElement();

        SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getInstance();
        dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss");

        for (Iterator logIterator = root.elementIterator("Log"); logIterator.hasNext();) {
            Element log = (Element) logIterator.next();

            Post post = new Post();
            for (Iterator iter = log.elementIterator(); iter.hasNext();) {
                Element element = (Element) iter.next();

                if (element.getName().equals("Title")) {
                    post.title = element.getText();
                }
                if (element.getName().equals("Content")) {
                    post.content = element.getText();
                }
                if (element.getName().equals("Tags")) {
                    post.setTags(element.getText().replace(' ', ','));
                }
                if (element.getName().equals("LogDate")) {
                    Date date = dateFormat.parse(element.getText());
                    post.date = date;
                    post.initialDate = date;
                }
                if (element.getName().equals("Title")) {
                    post.title = element.getText();
                }
            }

            post.insert();

            for (Iterator commentIterator = log.element("Comments").elementIterator("Comment"); commentIterator
                    .hasNext();) {
                Element commentElement = (Element) commentIterator.next();

                Comment comment = new Comment();
                comment.postId = post.id;

                for (Iterator iter = commentElement.elementIterator(); iter.hasNext();) {
                    Element element = (Element) iter.next();

                    if (element.getName().equals("Email")) {
                        comment.email = element.getText();
                    }
                    if (element.getName().equals("HomePage")) {
                        comment.link = element.getText();
                    }
                    if (element.getName().equals("NiceName")) {
                        comment.by = element.getText();
                    }
                    if (element.getName().equals("CreateTime")) {
                        Date date = dateFormat.parse(element.getText());
                        comment.date = date;
                    }
                    if (element.getName().equals("CommentText")) {
                        comment.content = element.getText();
                    }
                }
                post.addComment(comment);
            }
        }

        showList();
    }

    public static void replyToComment(long id, @Required String content) {
        checkAdmin();
        if (Validation.hasErrors()) {
            params.flash();
            Validation.keep();
            commentReplyForm(id);
        }

        Comment comment = Comment.findById(id);
        if (comment == null) {
            notFound();
        }
        Post post = Post.findById(comment.postId);
        if (post == null) {
            notFound();
        }
        comment.reply(content);
        Mails.commentReplied(comment, post);
        showPost(post.id);
    }

    public static void commentReplyForm(long id) {
        checkAdmin();
        Comment comment = Comment.findById(id);
        if (comment == null) {
            notFound();
        }
        render(comment);
    }

    public static void deleteComment(long id) {
        checkAdmin();
        Comment comment = Comment.findById(id);
        if (comment == null) {
            notFound();
        }
        Post post = Post.findById(comment.postId);
        if (post == null) {
            comment.delete();
        } else {
            post.deleteComment(id);
        }
        showPost(post.id);
    }

    public static void export() throws IOException {
        Query<Post> query = Post.all();
        List<Post> posts = query.fetch();
        response.contentType = "octet/gzip"; // TODO: what's the correct MIME type?
        GZIPOutputStream zip = new GZIPOutputStream(response.out);
        int offset = 0;
        for (Post post : posts) {
            zip.write(post.toString().getBytes(Charset.forName("UTF-8")));
            zip.write('\r');
        }
        zip.close();
    }

    public static void showTagCloud() {
        List<Tag> tags = Tag.all().fetch();
        render(tags);
    }

    public static void refreshTags() {
        for (Tag tag : Tag.all().fetch()) {
            tag.delete();
        }
        for (Post post : Post.all().fetch()) {
            if (post.tags != null) {
                for (String tag : post.tags) {
                    Tag.use(tag);
                }
            }
        }
    }
}