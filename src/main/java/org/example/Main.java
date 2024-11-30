package org.example;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class Main {

    private static WebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    public static void setUp() {

        io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();

        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void acceptCookies() {
        wait.until(ExpectedConditions.urlMatches("https://(www\\.)?mts\\.by/"));

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement cookieAgreeButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("cookie-agree")));
                cookieAgreeButton.click();
                break;
            } catch (Exception e) {
                driver.navigate().refresh();
            }
        }
    }

    @Test
    public void testPaymentSystemLogos() {
        driver.get("https://mts.by");
        acceptCookies();
        WebElement visaLogo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='pay-section']//img[@alt='Visa']")));
        WebElement mastercardLogo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='pay-section']//img[@alt='MasterCard']")));
        WebElement belkartLogo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='pay-section']//img[@alt='Белкарт']")));
        assertNotNull(visaLogo);
        assertNotNull(mastercardLogo);
        assertNotNull(belkartLogo);
    }

    @Test
    void checkPayFormName() {
        driver.get("https://mts.by");
        acceptCookies();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement h2Element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class='pay__wrapper']/h2")));
        Assertions.assertEquals(h2Element.getText(), "Онлайн пополнение\nбез комиссии", "Ошибка: наименование неверное");
    }
    @Test
    public void testMoreAboutServiceLink() {
        driver.get("https://mts.by");
        acceptCookies();
        WebElement linkElement = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Подробнее о сервисе")));
        assertNotNull(linkElement);
        linkElement.click();

        WebElement breadcrumbElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//span[@itemprop='name' and text()='Порядок оплаты и безопасность интернет платежей']")));
        assertNotNull(breadcrumbElement);
        assertEquals("Порядок оплаты и безопасность интернет платежей", breadcrumbElement.getText());
    }

    @Test
    public void testContinueButton() {
        driver.get("https://mts.by");
        acceptCookies();

        WebElement phoneInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("connection-phone")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", phoneInput);

        phoneInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("connection-phone")));
        WebElement amountInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("connection-sum")));
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Продолжить']")));

        phoneInput.sendKeys("297777777");
        amountInput.sendKeys("100");
        continueButton.click();

        checkForNewWindowOrIframe();

        try {
            WebElement iframe = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("iframe.bepaid-iframe")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", iframe);
            driver.switchTo().frame(iframe);

            WebElement cardNumberInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#cc-number")));
            WebElement cvcInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[formcontrolname='cvc']")));
            WebElement expirationDateInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.date-input")));
            WebElement cardHolderInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[autocomplete='cc-name']")));

            assertTrue(cardNumberInput.isDisplayed(), "Поле ввода номера карты не отображается");
            assertTrue(cvcInput.isDisplayed(), "Поле ввода CVC не отображается");
            assertTrue(expirationDateInput.isDisplayed(), "Поле ввода срока действия карты не отображается");
            assertTrue(cardHolderInput.isDisplayed(), "Поле ввода имени держателя карты не отображается");

            checkPaymentFormVisibility();
            checkAndFixZIndex();
            checkAndFixPositioning();
            checkAndFixStyles();

            driver.switchTo().defaultContent();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Ошибка при поиске элементов на странице оплаты: " + e.getMessage());
        }
    }

    private void checkForNewWindowOrIframe() {
        String originalWindow = driver.getWindowHandle();
        Set<String> allWindows = driver.getWindowHandles();

        if (allWindows.size() > 1) {
            for (String windowHandle : allWindows) {
                if (!originalWindow.contentEquals(windowHandle)) {
                    driver.switchTo().window(windowHandle);
                    break;
                }
            }
        } else {
            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            System.out.println("Found " + iframes.size() + " iframes on the page after clicking 'Продолжить'.");
            for (WebElement iframe : iframes) {
                System.out.println("Iframe src: " + iframe.getAttribute("src"));
                if (iframe.getAttribute("src").contains("checkout.bepaid.by")) {
                    driver.switchTo().frame(iframe);
                    break;
                }
            }
        }
    }

    private void checkPaymentFormVisibility() {
        WebElement paymentForm = driver.findElement(By.xpath("//div[contains(@class, 'card-page__card')]"));
        if (!paymentForm.isDisplayed()) {
            System.out.println("Форма оплаты не видна на странице.");
        } else {
            System.out.println("Форма оплаты видна на странице.");
        }
    }

    private void checkAndFixZIndex() {
        WebElement paymentForm = driver.findElement(By.xpath("//div[contains(@class, 'card-page__card')]"));
        String currentZIndex = paymentForm.getCssValue("z-index");
        if (currentZIndex.equals("auto") || Integer.parseInt(currentZIndex) < 1000) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.zIndex = '1000';", paymentForm);
            System.out.println("Изменен z-index формы оплаты.");
        }
    }

    private void checkAndFixPositioning() {
        WebElement paymentForm = driver.findElement(By.xpath("//div[contains(@class, 'card-page__card')]"));
        String currentPosition = paymentForm.getCssValue("position");
        if (!currentPosition.equals("relative")) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.position = 'relative';", paymentForm);
            System.out.println("Изменено позиционирование формы оплаты.");
        }
    }

    private void checkAndFixStyles() {
        WebElement paymentForm = driver.findElement(By.xpath("//div[contains(@class, 'card-page__card')]"));
        String currentDisplay = paymentForm.getCssValue("display");
        if (currentDisplay.equals("none")) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.display = 'block';", paymentForm);
            System.out.println("Изменен стиль отображения формы оплаты.");
        }
    }
}