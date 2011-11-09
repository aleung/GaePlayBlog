package models.blog;

import siena.Column;
import siena.Generator;
import siena.Id;
import siena.Model;
import siena.Query;

public class Tag extends Model {

    @Id(Generator.AUTO_INCREMENT)
    public Long id;

    @Column("name")
    public String name;

    @Column("count")
    public int count;

    public Tag(String name) {
        this.name = name;
        this.count = 1;
    }

    public void increaseCount() {
        count++;
        update();
    }

    public void decreaseCount() {
        if (count > 0) {
            count--;
        }
        update();
    }

    public static Query<Tag> all() {
        return Model.all(Tag.class);
    }

    public static Tag find(String name) {
        return Tag.all().filter("name", name).get();
    }

    public static void use(String name) {
        Tag tag = Tag.find(name);
        if (tag == null) {
            new Tag(name).insert();
        } else {
            tag.increaseCount();
        }
    }

    public static void unuse(String name) {
        Tag tag = Tag.find(name);
        if (tag != null) {
            tag.decreaseCount();
        }
    }
}
