package com.laycoo.tourism.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.common.dto.UserBrief;
import com.laycoo.tourism.common.exception.BusinessException;
import com.laycoo.tourism.common.security.UserContext;
import com.laycoo.tourism.interaction.dto.CommentRequest;
import com.laycoo.tourism.interaction.dto.CommentVO;
import com.laycoo.tourism.interaction.entity.Comment;
import com.laycoo.tourism.interaction.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final ContentGateway contentGateway;
    private final UserGateway userGateway;

    public CommentVO add(Long userId, CommentRequest request) {
        contentGateway.require(request.getContentId());
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException("评论内容不能为空");
        }
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setContentId(request.getContentId());
        comment.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        comment.setContent(request.getContent().trim());
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);
        contentGateway.increase(request.getContentId(), "comment", 1);
        return toVOs(List.of(comment), userId, false).get(0);
    }

    public PageResult<CommentVO> list(Long contentId, long page, long size, Long currentUserId) {
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), 50);
        Page<Comment> commentPage = commentMapper.selectPage(new Page<>(current, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getContentId, contentId)
                        .eq(Comment::getStatus, 1)
                        .orderByDesc(Comment::getCreateTime)
                        .orderByDesc(Comment::getId));
        return PageResult.of(toVOs(commentPage.getRecords(), currentUserId, false),
                commentPage.getTotal(), current, pageSize);
    }

    public PageResult<CommentVO> mine(Long userId, long page, long size) {
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), 50);
        Page<Comment> commentPage = commentMapper.selectPage(new Page<>(current, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, userId)
                        .eq(Comment::getStatus, 1)
                        .orderByDesc(Comment::getCreateTime));
        return PageResult.of(toVOs(commentPage.getRecords(), userId, true),
                commentPage.getTotal(), current, pageSize);
    }

    public void delete(Long commentId, Long userId, boolean admin) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() == null || comment.getStatus() != 1) {
            throw BusinessException.notFound("评论不存在");
        }
        if (!comment.getUserId().equals(userId) && !admin) {
            throw BusinessException.forbidden("只能删除自己的评论");
        }
        comment.setStatus(0);
        commentMapper.updateById(comment);
        contentGateway.increase(comment.getContentId(), "comment", -1);
    }

    public long count(Long contentId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getContentId, contentId)
                .eq(Comment::getStatus, 1));
    }

    private List<CommentVO> toVOs(List<Comment> comments, Long currentUserId, boolean withTitle) {
        List<CommentVO> result = new ArrayList<>();
        if (comments == null || comments.isEmpty()) {
            return result;
        }
        Map<Long, UserBrief> users = userGateway.batch(comments.stream().map(Comment::getUserId).toList());
        Map<Long, ContentCard> contents = new LinkedHashMap<>();
        if (withTitle) {
            for (ContentCard card : contentGateway.batch(comments.stream().map(Comment::getContentId).toList())) {
                contents.put(card.getId(), card);
            }
        }
        boolean admin = UserContext.isAdmin();
        for (Comment comment : comments) {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setUserId(comment.getUserId());
            vo.setContentId(comment.getContentId());
            vo.setParentId(comment.getParentId());
            vo.setContent(comment.getContent());
            vo.setCreateTime(comment.getCreateTime());
            UserBrief user = users.get(comment.getUserId());
            vo.setNickname(user == null || !StringUtils.hasText(user.getNickname())
                    ? "匿名旅人" : user.getNickname());
            vo.setAvatar(user == null ? "" : user.getAvatar());
            if (withTitle && contents.containsKey(comment.getContentId())) {
                vo.setContentTitle(contents.get(comment.getContentId()).getTitle());
            }
            boolean mine = currentUserId != null && currentUserId.equals(comment.getUserId());
            vo.setMine(mine);
            vo.setDeletable(mine || admin);
            result.add(vo);
        }
        return result;
    }
}
