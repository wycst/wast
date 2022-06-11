package io.github.wycst.wast.clients.redis.data.entry;

/**
 * @Author: wangy
 * @Date: 2020/6/28 16:28
 * @Description:
 */
public class ZSetMember {

    private double score;
    private String member;

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    @Override
    public String toString() {
        return "ZSetMember{" +
                "score=" + score +
                ", member='" + member + '\'' +
                '}';
    }
}
