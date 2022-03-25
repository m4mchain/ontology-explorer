package com.github.ontio.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.ontio.config.ParamsConfig;
import com.github.ontio.mapper.*;
import com.github.ontio.model.common.PageResponseBean;
import com.github.ontio.model.common.ResponseBean;
import com.github.ontio.model.dao.OepLogo;
import com.github.ontio.model.dto.*;
import com.github.ontio.model.dto.ranking.TokenRankingDto;
import com.github.ontio.service.ITokenService;
import com.github.ontio.util.ConstantParam;
import com.github.ontio.util.ErrorInfo;
import com.github.ontio.util.Helper;
import com.github.ontio.util.external.CoinMarketCapApi;
import com.github.ontio.util.external.CoinMarketCapQuotes;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zhouq
 * @version 1.0
 * @date 2019/5/8
 */
@Service("TokenService")
@Slf4j
public class TokenServiceImpl implements ITokenService {

    private static final String TOKEN_SORT_PATTERN = "-?(address_count|tx_count|create_time)";

    private final Oep4Mapper oep4Mapper;
    private final Oep5Mapper oep5Mapper;
    private final Oep8Mapper oep8Mapper;
    private final Orc20Mapper orc20Mapper;
    private final Orc721Mapper orc721Mapper;
    private final Orc1155Mapper orc1155Mapper;
    private final Oep8TxDetailMapper oep8TxDetailMapper;
    private final TokenDailyAggregationMapper tokenDailyAggregationMapper;
    private final RankingMapper rankingMapper;
    private final CoinMarketCapApi coinMarketCapApi;
    private final OepLogoMapper oepLogoMapper;

    @Autowired
    public TokenServiceImpl(Oep4Mapper oep4Mapper, Oep5Mapper oep5Mapper, Oep8Mapper oep8Mapper,
                            Oep8TxDetailMapper oep8TxDetailMapper, Orc20Mapper orc20Mapper, Orc721Mapper orc721Mapper, Orc1155Mapper orc1155Mapper, TokenDailyAggregationMapper tokenDailyAggregationMapper,
                            RankingMapper rankingMapper, CoinMarketCapApi coinMarketCapApi, OepLogoMapper oepLogoMapper) {
        this.oep4Mapper = oep4Mapper;
        this.oep5Mapper = oep5Mapper;
        this.oep8Mapper = oep8Mapper;
        this.orc20Mapper = orc20Mapper;
        this.orc721Mapper = orc721Mapper;
        this.orc1155Mapper = orc1155Mapper;
        this.oep8TxDetailMapper = oep8TxDetailMapper;
        this.tokenDailyAggregationMapper = tokenDailyAggregationMapper;
        this.rankingMapper = rankingMapper;
        this.coinMarketCapApi = coinMarketCapApi;
        this.oepLogoMapper = oepLogoMapper;
    }

    @Override
    public ResponseBean queryTokensByPage(String tokenType, Integer pageNumber, Integer pageSize, List<String> sorts) {

        int total = 0;
        PageResponseBean pageResponseBean = new PageResponseBean(new ArrayList(), total);
        List<String> ascending;
        List<String> descending;
        if (sorts == null || sorts.isEmpty()) {
            ascending = null;
            descending = Collections.singletonList("create_time");
        } else {
            ascending = sorts.stream()
                    .filter(sort -> Pattern.matches(TOKEN_SORT_PATTERN, sort) && !sort.startsWith("-"))
                    .collect(Collectors.toList());
            descending = sorts.stream()
                    .filter(sort -> Pattern.matches(TOKEN_SORT_PATTERN, sort) && sort.startsWith("-"))
                    .map(sort -> sort.substring(1))
                    .collect(Collectors.toList());
        }

        switch (tokenType.toLowerCase()) {
            case ConstantParam.ASSET_TYPE_OEP4:
                PageHelper.startPage(pageNumber, pageSize);
                List<Oep4DetailDto> oep4DetailDtos = oep4Mapper.selectOep4Tokens(ascending, descending);
                total = ((Long) ((Page) oep4DetailDtos).getTotal()).intValue();
                pageResponseBean.setTotal(total);
                pageResponseBean.setRecords(oep4DetailDtos);
                break;
            case ConstantParam.ASSET_TYPE_OEP5:
                PageHelper.startPage(pageNumber, pageSize);
                List<Oep5DetailDto> oep5DetailDtos = oep5Mapper.selectOep5Tokens(ascending, descending);
                total = ((Long) ((Page) oep5DetailDtos).getTotal()).intValue();
                pageResponseBean.setTotal(total);
                pageResponseBean.setRecords(oep5DetailDtos);
                break;
            case ConstantParam.ASSET_TYPE_OEP8:
                PageHelper.startPage(pageNumber, pageSize);
                List<Oep8DetailDto> oep8DetailDtos = oep8Mapper.selectOep8Tokens(ascending, descending);
                total = ((Long) ((Page) oep8DetailDtos).getTotal()).intValue();
                //OEP8同一个合约hash有多种token，需要根据tokenId分类
                oep8DetailDtos.forEach(item -> {
                    item = formatOep8DetailDto(item);
                });

                pageResponseBean.setTotal(total);
                pageResponseBean.setRecords(oep8DetailDtos);
                break;
            case ConstantParam.ASSET_TYPE_ORC20:
                PageHelper.startPage(pageNumber, pageSize);
                List<Orc20DetailDto> orc20DetailDtos = orc20Mapper.selectOrc20Tokens(ascending, descending);
                total = ((Long) ((Page) orc20DetailDtos).getTotal()).intValue();
                pageResponseBean.setTotal(total);
                pageResponseBean.setRecords(orc20DetailDtos);
                break;
            case ConstantParam.ASSET_TYPE_ORC721:
                PageHelper.startPage(pageNumber, pageSize);
                List<Orc721DetailDto> orc721DetailDtos = orc721Mapper.selectOrc721Tokens(ascending, descending);
                total = ((Long) ((Page) orc721DetailDtos).getTotal()).intValue();
                pageResponseBean.setTotal(total);
                pageResponseBean.setRecords(orc721DetailDtos);
                break;
            case ConstantParam.ASSET_TYPE_ORC1155:
                PageHelper.startPage(pageNumber, pageSize);
                List<Orc1155DetailDto> orc1155DetailDtos = orc1155Mapper.selectOrc1155Tokens(ascending, descending);
                total = ((Long) ((Page) orc1155DetailDtos).getTotal()).intValue();
                //ORC1155同一个合约hash有多种token，需要根据tokenId分类
                orc1155DetailDtos.forEach(item -> {
                    formatOrc1155DetailDto(item);
                });
                pageResponseBean.setTotal(total);
                pageResponseBean.setRecords(orc1155DetailDtos);
                break;
        }

        return new ResponseBean(ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), pageResponseBean);
    }

    /**
     * 根据tokenid格式化oep8 token信息
     *
     * @param oep8DetailDto
     * @return
     */
    private Oep8DetailDto formatOep8DetailDto(Oep8DetailDto oep8DetailDto) {

        Map tokenIdMap = new HashMap();
        Map totalSupplyMap = new HashMap();
        Map symbolMap = new HashMap();
        Map tokenNameMap = new HashMap();

        String[] tokenIds = ((String) oep8DetailDto.getTokenId()).split(",");
        String[] totalSupplys = ((String) oep8DetailDto.getTotalSupply()).split(",");
        String[] symbols = ((String) oep8DetailDto.getSymbol()).split(",");
        String[] tokenNames = ((String) oep8DetailDto.getTokenName()).split(",");

        for (int i = 0; i < tokenIds.length; i++) {
            tokenIdMap.put(tokenIds[i], tokenIds[i]);
            totalSupplyMap.put(tokenIds[i], totalSupplys[i]);
            symbolMap.put(tokenIds[i], symbols[i]);
            tokenNameMap.put(tokenIds[i], tokenNames[i]);
        }
        oep8DetailDto.setTotalSupply(totalSupplyMap);
        oep8DetailDto.setSymbol(symbolMap);
        oep8DetailDto.setTokenName(tokenNameMap);
        oep8DetailDto.setTokenId(tokenIdMap);

        return oep8DetailDto;
    }

    /**
     * 根据tokenid格式化orc1155 token信息
     *
     * @param orc1155DetailDto
     * @return
     */
    private Orc1155DetailDto formatOrc1155DetailDto(Orc1155DetailDto orc1155DetailDto) {

        Map tokenIdMap = new HashMap();
        Map totalSupplyMap = new HashMap();
        Map symbolMap = new HashMap();
        Map tokenNameMap = new HashMap();

        String[] tokenIds = ((String) orc1155DetailDto.getTokenId()).split(",");
        String[] totalSupplys = ((String) orc1155DetailDto.getTotalSupply()).split(",");
        String[] symbols = ((String) orc1155DetailDto.getSymbol()).split(",");
        String[] tokenNames = ((String) orc1155DetailDto.getTokenName()).split(",");

        for (int i = 0; i < tokenIds.length; i++) {
            tokenIdMap.put(tokenIds[i], tokenIds[i]);
            totalSupplyMap.put(tokenIds[i], totalSupplys[i]);
            symbolMap.put(tokenIds[i], symbols[i]);
            tokenNameMap.put(tokenIds[i], tokenNames[i]);
        }
        orc1155DetailDto.setTotalSupply(totalSupplyMap);
        orc1155DetailDto.setSymbol(symbolMap);
        orc1155DetailDto.setTokenName(tokenNameMap);
        orc1155DetailDto.setTokenId(tokenIdMap);

        return orc1155DetailDto;
    }

    @Override
    public ResponseBean queryTokenDetail(String tokenType, String contractHash) {

        Object obj = new Object();
        String contactInfo;
        switch (tokenType.toLowerCase()) {
            case ConstantParam.ASSET_TYPE_OEP4:
                Oep4DetailDto oep4DetailDto = oep4Mapper.selectOep4TokenDetail(contractHash);
                contactInfo = oep4DetailDto.getContactInfo();
                contactInfo = formatContactInfo(contactInfo);
                oep4DetailDto.setContactInfo(contactInfo);
                obj = oep4DetailDto;
                break;
            case ConstantParam.ASSET_TYPE_OEP5:
                Oep5DetailDto oep5DetailDto = oep5Mapper.selectOep5TokenDetail(contractHash);
                contactInfo = oep5DetailDto.getContactInfo();
                contactInfo = formatContactInfo(contactInfo);
                oep5DetailDto.setContactInfo(contactInfo);
                obj = oep5DetailDto;
                break;
            case ConstantParam.ASSET_TYPE_OEP8:
                Oep8DetailDto oep8DetailDto = oep8Mapper.selectOep8TokenDetail(contractHash);
                if (Helper.isNotEmptyAndNull(oep8DetailDto)) {
                    formatOep8DetailDto(oep8DetailDto);
                }
                contactInfo = oep8DetailDto.getContactInfo();
                contactInfo = formatContactInfo(contactInfo);
                oep8DetailDto.setContactInfo(contactInfo);
                obj = oep8DetailDto;
                break;
            case ConstantParam.ASSET_TYPE_ORC20:
                Orc20DetailDto orc20DetailDto = orc20Mapper.selectOrc20TokenDetail(contractHash);
                contactInfo = orc20DetailDto.getContactInfo();
                contactInfo = formatContactInfo(contactInfo);
                orc20DetailDto.setContactInfo(contactInfo);
                obj = orc20DetailDto;
                break;
            case ConstantParam.ASSET_TYPE_ORC721:
                Orc721DetailDto orc721DetailDto = orc721Mapper.selectOrc721TokenDetail(contractHash);
                contactInfo = orc721DetailDto.getContactInfo();
                contactInfo = formatContactInfo(contactInfo);
                orc721DetailDto.setContactInfo(contactInfo);
                obj = orc721DetailDto;
                break;
            case ConstantParam.ASSET_TYPE_ORC1155:
                Orc1155DetailDto orc1155DetailDto = orc1155Mapper.selectOrc1155TokenDetail(contractHash);
                if (Helper.isNotEmptyAndNull(orc1155DetailDto)) {
                    formatOrc1155DetailDto(orc1155DetailDto);
                }
                contactInfo = orc1155DetailDto.getContactInfo();
                contactInfo = formatContactInfo(contactInfo);
                orc1155DetailDto.setContactInfo(contactInfo);
                obj = orc1155DetailDto;
                break;
        }
        if (Helper.isEmptyOrNull(obj)) {
            return new ResponseBean(ErrorInfo.NOT_FOUND.code(), ErrorInfo.NOT_FOUND.desc(), false);
        }
        return new ResponseBean(ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), obj);
    }

    private String formatContactInfo(String contactInfo) {
        try {
            if (!StringUtils.isEmpty(contactInfo)) {
                JSONObject jsonObject = JSONObject.parseObject(contactInfo);
                String website = jsonObject.getString("website");
                if (StringUtils.isEmpty(website)) {
                    jsonObject.remove("website");
                }
                String email = jsonObject.getString("email");
                if (StringUtils.isEmpty(email)) {
                    jsonObject.remove("email");
                }
                return JSON.toJSONString(jsonObject);
            }
        } catch (Exception e) {
            log.error("parse contractDetail error:{}", e.getMessage());
        }
        return contactInfo;
    }

    @Override
    public ResponseBean queryOep8TxsByPage(String contractHash, String tokenName, Integer pageNumber, Integer pageSize) {

        int start = pageSize * (pageNumber - 1) < 0 ? 0 : pageSize * (pageNumber - 1);

        List<TxDetailDto> txDetailDtos = oep8TxDetailMapper.selectTxsByCalledContractHashAndTokenName(contractHash, tokenName,
                start, pageSize);
        Integer count = oep8TxDetailMapper.selectCountByCalledContracthashAndTokenName(contractHash, tokenName);

        PageResponseBean pageResponseBean = new PageResponseBean(txDetailDtos, count);

        return new ResponseBean(ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), pageResponseBean);
    }

    @Override
    public ResponseBean queryDailyAggregations(String tokenType, String contractHash, Date from, Date to) {
        Object result = null;
        if (ConstantParam.ASSET_TYPE_OEP4.equalsIgnoreCase(tokenType)) {
            result = tokenDailyAggregationMapper.findAggregations(contractHash, from, to);
        } else if (ConstantParam.ASSET_TYPE_ORC20.equalsIgnoreCase(tokenType)) {
            result = tokenDailyAggregationMapper.findEVMAggregations(contractHash, from, to);
        }
        return new ResponseBean(ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), result);
    }

    @Override
    public ResponseBean queryRankings(List<Short> rankingIds, short duration) {
        List<TokenRankingDto> rankings = rankingMapper.findTokenRankings(rankingIds, duration);
        Map<Short, List<TokenRankingDto>> rankingMap =
                rankings.stream().collect(Collectors.groupingBy(TokenRankingDto::getRankingId));
        return new ResponseBean(ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), rankingMap);
    }

    @Override
    public ResponseBean queryPrice(String token, String fiat) {
        String key = token + "-" + fiat;
        CoinMarketCapQuotes quotes = tokenQuotes.get(key);
        TokenPriceDto dto = TokenPriceDto.from(token, quotes);
        return new ResponseBean(ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), dto);
    }

    private LoadingCache<String, CoinMarketCapQuotes> tokenQuotes;

    @Autowired
    public void setParamsConfig(ParamsConfig paramsConfig) {
        tokenQuotes = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(paramsConfig.getCoinMarketCapRefreshInterval()))
                .build(key -> {
                    String[] parts = key.split("-");
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("invalid token-fiat pair: " + key);
                    }
                    return coinMarketCapApi.getQuotes(parts[0], parts[1]);
                });
    }


    @Override
    public ResponseBean queryOepLogos(String contractHash, int pageSize, int pageNumber) {
        PageHelper.startPage(pageNumber, pageSize);
        Example example = new Example(OepLogo.class);
        example.and()
                .andEqualTo("contractHash", contractHash);
        example.orderBy("id").desc();
        List<OepLogo> oepLogos = oepLogoMapper.selectByExample(example);
        Long total = ((Page) oepLogos).getTotal();
        PageResponseBean responseBean = new PageResponseBean(oepLogos, total.intValue());
        return new ResponseBean(ErrorInfo.SUCCESS.code(), ErrorInfo.SUCCESS.desc(), responseBean);
    }
}
