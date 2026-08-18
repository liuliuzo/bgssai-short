package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;

@Mapper
public interface ShortFilmMapper {
    long countByExample(ShortFilmExample example);
    int insert(ShortFilm row);
    int insertSelective(ShortFilm row);
    List<ShortFilm> selectByExample(ShortFilmExample example);
    ShortFilm selectOneByExample(ShortFilmExample example);
    ShortFilm selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") ShortFilm row, @Param("example") ShortFilmExample example);
    int updateByPrimaryKeySelective(ShortFilm row);
}
