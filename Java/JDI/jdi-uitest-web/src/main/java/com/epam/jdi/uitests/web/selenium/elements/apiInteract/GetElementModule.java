package com.epam.jdi.uitests.web.selenium.elements.apiInteract;
/*
 * Copyright 2004-2016 EPAM Systems
 *
 * This file is part of JDI project.
 *
 * JDI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JDI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JDI. If not, see <http://www.gnu.org/licenses/>.
 */


import com.epam.commons.LinqUtils;
import com.epam.commons.Timer;
import com.epam.commons.pairs.Pair;
import com.epam.commons.pairs.Pairs;
import com.epam.jdi.uitests.core.interfaces.base.IAvatar;
import com.epam.jdi.uitests.core.interfaces.base.IBaseElement;
import com.epam.jdi.uitests.web.selenium.driver.WebDriverByUtils;
import com.epam.jdi.uitests.web.selenium.elements.BaseElement;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.epam.commons.LinqUtils.where;
import static com.epam.commons.PrintUtils.print;
import static com.epam.commons.ReflectionUtils.isClass;
import static com.epam.jdi.uitests.core.settings.JDISettings.*;
import static com.epam.jdi.uitests.web.settings.WebSettings.getDriverFactory;
import static java.lang.String.format;

/**
 * Created by Roman_Iovlev on 7/3/2015.
 */
public class GetElementModule implements IAvatar {
    private static final String FAILED_TO_FIND_ELEMENT_MESSAGE = "Can't find Element '%s' during %s seconds";
    private static final String FIND_TO_MUCH_ELEMENTS_MESSAGE = "Find %s elements instead of one for Element '%s' during %s seconds";
    public By byLocator;
    public Pairs<ContextType, By> context = new Pairs<>();
    public Function<WebElement, Boolean> localElementSearchCriteria = null;
    public WebElement rootElement;
    private String driverName = "";
    private IBaseElement element;
    private List<WebElement> webElements;

    public GetElementModule(IBaseElement element) {
        this.element = element;
        driverName = driverFactory.currentDriverName();
    }

    public GetElementModule(By byLocator, IBaseElement element) {
        this(element);
        this.byLocator = byLocator;
    }

    public GetElementModule(By byLocator, Pairs<ContextType, By> context, IBaseElement element) {
        this(element);
        this.byLocator = byLocator;
        this.context = context;
    }

    public boolean hasLocator() {
        return byLocator != null;
    }

    public WebDriver getDriver() {
        return (WebDriver) driverFactory.getDriver(driverName);
    }
    public String getDriverName() {
        return driverName;
    }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public WebElement getElement() {
        logger.debug("Get Web Element: " + element);
        WebElement element = timer().getResultByCondition(this::getElementAction, el -> el != null);
        logger.debug("One Element found");
        return element;
    }

    public List<WebElement> getElements() {
        logger.debug("Get Web elements: " + element);
        List<WebElement> elements = getElementsAction();
        logger.debug("Found %s elements", elements.size());
        return elements;
    }

    public Timer timer() {
        return new Timer(timeouts.currentTimeoutSec * 1000);
    }

    private List<WebElement> getElementsAction() {
        if (webElements != null)
            return webElements;
        List<WebElement> result = timer().getResultByCondition(
                this::searchElements,
                els -> where(els, getSearchCriteria()).size() > 0);
        timeouts.dropTimeouts();
        if (result == null)
            throw exception("Can't get Web Elements");
        return where(result, el -> getSearchCriteria().apply(el));
    }

    private Function<WebElement, Boolean> getSearchCriteria() {
        return localElementSearchCriteria != null ? localElementSearchCriteria : getDriverFactory().elementSearchCriteria;
    }

    public GetElementModule searchAll() {
        localElementSearchCriteria = el -> el != null;
        return this;
    }

    private WebElement getElementAction() {
        int timeout = timeouts.currentTimeoutSec;
        List<WebElement> result = getElementsAction();
        switch (result.size()) {
            case 0:
                throw exception(FAILED_TO_FIND_ELEMENT_MESSAGE, element, timeout);
            case 1:
                return result.get(0);
            default:
                throw exception(FIND_TO_MUCH_ELEMENTS_MESSAGE, result.size(), element, timeout);
        }
    }

    private SearchContext getSearchContext(BaseElement element) {
        SearchContext searchContext = isClass(element.getParent().getClass(), BaseElement.class)
                ? getSearchContext((BaseElement) element.getParent())
                : getDriver().switchTo().defaultContent();
        return element.getLocator() != null
                ? searchContext.findElement(correctXPaths(element.getLocator()))
                : searchContext;
    }
    private List<WebElement> searchElements()
    {
        Object p = element.getParent();
        BaseElement parent = p != null && isClass(p.getClass(), BaseElement.class)
            ? (BaseElement) p
            : null;
        SearchContext context = parent != null
                ? getSearchContext(parent)
                : getDriver().switchTo().defaultContent();
        return context.findElements(correctXPaths(byLocator));
    }

    private By correctXPaths(By byValue) {
        return byValue.toString().contains("By.xpath: //")
                ? WebDriverByUtils.getByFunc(byValue).apply(WebDriverByUtils.getByLocator(byValue)
                .replaceFirst("/", "./"))
                : byValue;
    }

    public void clearCookies() {
        getDriver().manage().deleteAllCookies();
    }

    @Override
    public String toString() {
        return shortLogMessagesFormat
                ? printFullLocator()
                : format("Locator: '%s'", byLocator)
                + ((context.size() > 0)
                        ? format(", Context: '%s'", context)
                        : "");
    }

    private String printFullLocator() {
        if (byLocator == null)
            return "No Locators";
        List<String> result = new ArrayList<>();
        if (context.size() != 0)
            result = LinqUtils.select(context, el -> printShortBy(el.value));
        result.add(printShortBy(byLocator));
        return print(result);
    }

    private String printShortBy(By by) {
        return String.format("%s='%s'", WebDriverByUtils.getByName(by), WebDriverByUtils.getByLocator(by));
    }
}