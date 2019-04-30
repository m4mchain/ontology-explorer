package com.github.ontio.mapper;

import com.github.ontio.model.dao.TxEventLog;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

public interface TxEventLogMapper extends Mapper<TxEventLog> {

    void deleteByHeight(@Param("blockHeight") int height);

    int selectCountByHeight(@Param("blockHeight") int height);

}