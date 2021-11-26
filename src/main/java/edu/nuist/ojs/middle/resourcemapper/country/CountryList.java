package edu.nuist.ojs.middle.resourcemapper.country;

import cn.hutool.core.map.MapUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

@XmlRootElement(name = "countries")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountryList {
	@XmlElement(name = "country")
    private List<Country> cityList;

	public TreeMap<String, String> getCityList(boolean isZh ) {
        HashMap<String, String>  rst = new HashMap<String, String>();
        for(Country c : cityList){
            rst.put( isZh? c.getZh(): c.getEn(), c.getCode() );
        }

		return MapUtil.sort(rst);
	}

	public void setCityList(List<Country> cityList) {
		this.cityList = cityList;
	}

}