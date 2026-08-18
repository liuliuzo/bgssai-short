package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;

@Mapper
public interface ShortEpisodeMapper {
    long countByExample(ShortEpisodeExample example);
    int insert(ShortEpisode row);
    int insertSelective(ShortEpisode row);
    List<ShortEpisode> selectByExample(ShortEpisodeExample example);
    ShortEpisode selectOneByExample(ShortEpisodeExample example);
    ShortEpisode selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortEpisode row, @Param("example") ShortEpisodeExample example);
    int updateByPrimaryKeySelective(ShortEpisode row);
}
