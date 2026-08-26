package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortEpisodeGap;
import com.bgssai.shortai.model.ShortEpisodeGapExample;

@Mapper
public interface ShortEpisodeGapMapper {
    long countByExample(ShortEpisodeGapExample example);
    int insert(ShortEpisodeGap row);
    int insertSelective(ShortEpisodeGap row);
    List<ShortEpisodeGap> selectByExample(ShortEpisodeGapExample example);
    ShortEpisodeGap selectOneByExample(ShortEpisodeGapExample example);
    ShortEpisodeGap selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortEpisodeGap row, @Param("example") ShortEpisodeGapExample example);
    int updateByPrimaryKeySelective(ShortEpisodeGap row);
}
