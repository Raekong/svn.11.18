package edu.nuist.ojs.middle.resourcemapper.country;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "country")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Country {
	@XmlAttribute(name = "en")
	private String en;

    @XmlAttribute(name = "zh")
	private String zh;

	@XmlAttribute(name = "code")
	private String code;

}
