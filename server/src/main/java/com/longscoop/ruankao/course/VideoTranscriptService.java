package com.longscoop.ruankao.course;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.longscoop.ruankao.course.persistence.VideoEntity;
import com.longscoop.ruankao.course.persistence.VideoMapper;
import com.longscoop.ruankao.course.persistence.VideoTranscriptSegmentEntity;
import com.longscoop.ruankao.course.persistence.VideoTranscriptSegmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VideoTranscriptService {

    private final VideoMapper videoMapper;
    private final VideoTranscriptSegmentMapper segmentMapper;

    public VideoTranscriptService(VideoMapper videoMapper, VideoTranscriptSegmentMapper segmentMapper) {
        this.videoMapper = videoMapper;
        this.segmentMapper = segmentMapper;
    }

    @Transactional
    public void replace(long videoId, List<Segment> segments) {
        requireVideo(videoId);
        if (segments == null) {
            throw new IllegalArgumentException("segments are required");
        }
        segments.forEach(this::validate);

        segmentMapper.delete(Wrappers.<VideoTranscriptSegmentEntity>lambdaQuery()
                .eq(VideoTranscriptSegmentEntity::getVideoId, videoId));
        for (Segment segment : segments) {
            VideoTranscriptSegmentEntity entity = new VideoTranscriptSegmentEntity();
            entity.setVideoId(videoId);
            entity.setStartSecond(segment.startSecond());
            entity.setEndSecond(segment.endSecond());
            entity.setContent(segment.content().trim());
            segmentMapper.insert(entity);
        }
    }

    @Transactional(readOnly = true)
    public List<VideoTranscriptSegmentEntity> list(long videoId) {
        requireVideo(videoId);
        return segmentMapper.selectList(Wrappers.<VideoTranscriptSegmentEntity>lambdaQuery()
                .eq(VideoTranscriptSegmentEntity::getVideoId, videoId)
                .orderByAsc(VideoTranscriptSegmentEntity::getStartSecond)
                .orderByAsc(VideoTranscriptSegmentEntity::getId));
    }

    private void validate(Segment segment) {
        if (segment == null || segment.startSecond() == null || segment.endSecond() == null
                || segment.startSecond().compareTo(BigDecimal.ZERO) < 0
                || segment.endSecond().compareTo(segment.startSecond()) <= 0) {
            throw new IllegalArgumentException("invalid transcript time range");
        }
        if (segment.content() == null || segment.content().trim().isEmpty()) {
            throw new IllegalArgumentException("transcript content is required");
        }
    }

    private void requireVideo(long videoId) {
        if (videoId <= 0 || videoMapper.selectById(videoId) == null) {
            throw new IllegalArgumentException("video must exist");
        }
    }

    public record Segment(BigDecimal startSecond, BigDecimal endSecond, String content) {
    }
}
