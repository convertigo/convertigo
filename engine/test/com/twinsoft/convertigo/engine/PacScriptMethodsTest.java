/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class PacScriptMethodsTest {
	private PacScriptMethods methods;
	@Before public void setup() {
		Engine.logProxyManager = Logger.getLogger("pac-helpers-test");
		methods = new PacScriptMethods();
	}
	private void at(int year, int month, int day, int hour, String zone) {
		var calendar=Calendar.getInstance(TimeZone.getTimeZone(zone)); calendar.clear(); calendar.set(year,month-1,day,hour,30,0);methods.setCurrentTime(calendar);
	}
	private void script(String declarations, String condition) {
		var pac = new PacManager("") {
			@Override public String getScriptContent() { return declarations + "\nfunction FindProxyForURL(url,host) { return ("+condition+") ? 'PROXY expected.invalid:3128' : 'DIRECT'; }"; }
		};
		pac.start();
		var result=pac.getPacInfos("https://service.example/path", "service.example");
		assertNotNull(condition,result); assertEquals("expected.invalid",result.getServer());
	}
	@Test public void ipv4MasksOperateOnBits() {
		assertTrue(methods.isInNet("10.127.1.2","10.0.0.0","255.128.0.0"));
		assertFalse(methods.isInNet("10.129.1.2","10.0.0.0","255.128.0.0"));
		assertTrue(methods.isInNet("203.0.113.5","0.0.0.0","0.0.0.0"));
	}
	@Test public void subnetChecksResolveHostNames() { assertTrue(methods.isInNet("localhost","127.0.0.0","255.0.0.0")); }
	@Test public void localNamesAreComparedOnLabelBoundaries() {
		assertTrue(methods.localHostOrDomainIs("www","www.example"));
		assertTrue(methods.localHostOrDomainIs("www.example","www.example"));
		assertFalse(methods.localHostOrDomainIs("www","www2.example"));
		assertFalse(methods.localHostOrDomainIs("www.example","www.example.invalid"));
	}
	@Test public void wildcardMatchesAreAnchoredAndSupportSingleCharacters() {
		assertTrue(methods.shExpMatch("api1.example","api?.example"));
		assertFalse(methods.shExpMatch("api12.example","api?.example"));
		assertFalse(methods.shExpMatch("not-api.example.invalid","api.example"));
		assertTrue(methods.shExpMatch("https://api.example/v1/a","https://*.example/*"));
	}
	@Test public void levelsCountEveryDot() { assertEquals(2,methods.dnsDomainLevels(".example.test")); }
	@Test public void legacyDnsReturnsIpv4OrNull() { assertNull(methods.dnsResolve("::1")); }
	@Test public void globalInitializersCanCallHelpers() { script("var local=myIpAddress();", "typeof local === 'string' && local.length > 0"); }
	@Test public void dateRangeAcceptsItsDocumentedArities() { script("", "dateRange(2000, 2100)"); }
	@Test public void timeRangeAcceptsItsDocumentedArities() { script("", "timeRange(0, 23)"); }
	@Test public void weekdayRangeAcceptsItsDocumentedArities() { script("", "weekdayRange('SUN', 'SAT')"); }
	@Test public void extendedHelpersAreRegistered() { script("", "typeof isInNetEx === 'function' && typeof dnsResolveEx === 'function' && typeof myIpAddressEx === 'function'"); }
	@Test public void datesWrapAcrossTheMonthBoundary() {
		at(2026,11,2,10,"GMT");assertTrue(methods.dateRange(20,5,null,null,null,null,null));
	}
	@Test public void timesWrapAcrossMidnight() {
		at(2026,11,2,1,"GMT");assertTrue(methods.timeRange(23,2,null,null,null,null,null));
	}
	@Test public void gmtUsesTheSameInstantInTheRequestedZone() {
		at(2026,11,2,1,"GMT+02:00");assertTrue(methods.timeRange(23,"GMT",null,null,null,null,null));
	}
	@Test public void simpleNameAndDomainTestsRemainCompatible() {
		assertTrue(methods.isPlainHostName("intranet")); assertFalse(methods.isPlainHostName("intranet.example"));
		assertTrue(methods.dnsDomainIs("api.example",".example"));assertFalse(methods.dnsDomainIs("api.example.invalid",".example"));
	}

	@Test public void extendedSubnetsValidatePrefixesAndAddressFamilies() {
		assertTrue(methods.isInNetEx("192.0.2.129", "192.0.2.128/25"));
		assertFalse(methods.isInNetEx("192.0.2.127", "192.0.2.128/25"));
		assertTrue(methods.isInNetEx("2001:db8:8000::1", "2001:db8:8000::/33"));
		assertFalse(methods.isInNetEx("2001:db8:7fff::1", "2001:db8:8000::/33"));
		assertTrue(methods.isInNetEx("fe80::1%12", "fe80::/10"));
		assertTrue(methods.isInNetEx("::ffff:192.0.2.5", "::ffff:192.0.2.0/120"));
		assertFalse(methods.isInNetEx("::ffff:192.0.2.5", "192.0.2.0/24"));
		for(String prefix:java.util.List.of("192.0.2.0/33","2001:db8::/129","192.0.2.0/-1","invalid/24","192.0.2.0/abc")) assertFalse(prefix,methods.isInNetEx("192.0.2.1",prefix));
		assertFalse(methods.isInNetEx("localhost", "127.0.0.0/8"));
		assertTrue(methods.isInNetEx("192.0.2.1", "0.0.0.0/0"));
	}
	@Test public void dnsAndLocalAddressListsSupportBothFamiliesWithoutDuplicates() throws Exception {
		var v4=java.net.InetAddress.getByName("192.0.2.5");var v6=java.net.InetAddress.getByName("2001:db8::5");
		var network=new PacScriptMethods() {
			@Override protected java.net.InetAddress[] resolve(String host) throws java.net.UnknownHostException {
				if(host.equals("missing.invalid"))throw new java.net.UnknownHostException();
				return host.equals("ipv6.invalid")?new java.net.InetAddress[]{v6}:new java.net.InetAddress[]{v4,v6,v4};
			}
			@Override protected java.util.List<java.net.InetAddress> localAddresses() {return java.util.List.of(v4,v6,v4);}
		};
		assertEquals("192.0.2.5",network.dnsResolve("dual.invalid"));assertNull(network.dnsResolve("ipv6.invalid"));
		assertFalse(network.isResolvable("ipv6.invalid"));assertTrue(network.isResolvableEx("ipv6.invalid"));
		assertNull(network.dnsResolve("missing.invalid"));assertEquals("",network.dnsResolveEx("missing.invalid"));
		assertEquals(v6.getHostAddress()+";192.0.2.5",network.dnsResolveEx("dual.invalid"));
		assertEquals(v6.getHostAddress()+";192.0.2.5",network.myIpAddressEx());
	}
	@Test public void helpersReturnJavascriptPrimitivesAndAllowStringOperations() {
		script("", "typeof dnsResolve('127.0.0.1') === 'string' && dnsResolve('127.0.0.1') === '127.0.0.1' && dnsResolve('::1') === null && dnsResolveEx('127.0.0.1').split(';')[0] === '127.0.0.1' && isInNetEx('2001:db8::1','2001:db8::/32') && isResolvableEx('::1')");
	}
	@Test public void allDateFormsIncludeTheirBoundaries() {
		at(2026,11,19,10,"GMT");
		assertTrue(methods.dateRange(19));assertTrue(methods.dateRange("NOV","GMT"));assertTrue(methods.dateRange(2026));
		assertTrue(methods.dateRange(19,"NOV"));assertTrue(methods.dateRange(19,"NOV",2026));
		assertTrue(methods.dateRange(1,"NOV",19,"NOV"));assertFalse(methods.dateRange(20,"NOV",30,"NOV"));
		assertTrue(methods.dateRange("OCT",2026,"NOV",2026));assertFalse(methods.dateRange("DEC",2026,"JAN",2027));
		assertTrue(methods.dateRange(1,"JAN",2026,19,"NOV",2026));
		assertFalse(methods.dateRange(29,"FEB",2025));assertFalse(methods.dateRange("INVALID"));
		at(2027,1,1,10,"GMT");assertTrue(methods.dateRange("DEC","JAN"));assertTrue(methods.dateRange(24,"DEC",5,"JAN"));
	}
	@Test public void timeAndWeekdayFormsIncludeMinutesSecondsAndGmt() {
		at(2026,11,19,10,"GMT");
		assertTrue(methods.timeRange(10));assertFalse(methods.timeRange(11));
		assertTrue(methods.timeRange(10,30,10,30));assertTrue(methods.timeRange(10,30,0,10,30,0));
		assertFalse(methods.timeRange(10,30,1,10,30,2));assertFalse(methods.timeRange(24));
		assertTrue(methods.weekdayRange("THU",null,"GMT"));assertTrue(methods.weekdayRange("THU"));assertTrue(methods.weekdayRange("THU","GMT"));assertFalse(methods.weekdayRange("FRI","MON"));
		at(2026,11,23,1,"GMT+02:00");assertTrue(methods.weekdayRange("SUN","GMT"));assertTrue(methods.dateRange(22,"GMT"));
	}
	@Test public void testClocksAreNotSharedBetweenPacInstances() {
		at(2040,1,1,10,"GMT");
		var other=new PacScriptMethods();var clock=Calendar.getInstance(TimeZone.getTimeZone("GMT"));clock.clear();clock.set(2041,0,1);other.setCurrentTime(clock);
		assertTrue(methods.dateRange(2040));assertTrue(other.dateRange(2041));
	}
}
