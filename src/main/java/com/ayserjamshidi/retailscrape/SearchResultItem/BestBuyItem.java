package com.ayserjamshidi.retailscrape.SearchResultItem;

import org.openqa.selenium.WebElement;

public class BestBuyItem extends WebItem {
	public BestBuyItem(WebElement element) {
		super();
	}

	@Override
	public boolean setImageSrc(WebElement element) {
		if (element != null) {
			this.imageSrc = element.getText();
			return true;
		}

		this.imageSrc = null;
		return false;
	}

	@Override
	public boolean setItemRating(WebElement element) {
		if (element != null) {
			this.rating = null; //element.e
			return true;
		}

		this.rating = null;
		return false;
	}

	@Override
	public boolean setListingName(WebElement element) {
		if (element != null) {

			return true;
		}

		this.listingName = null;
		return false;
	}

	@Override
	public boolean setPromotion(WebElement element) {
		if (element != null) {

			return true;
		}

		this.promotion = null;
		return false;
	}

	@Override
	public boolean setPriceBeforeDiscount(WebElement element) {
		if (element != null) {

			return true;
		}

		this.priceBeforeDiscount = null;
		return false;
	}

	@Override
	public boolean setPriceCurrent(WebElement element) {
		if (element != null) {

			return true;
		}

		this.currentPrice = null;
		return false;
	}

	@Override
	public boolean setPriceShipping(WebElement element) {
		if (element != null) {

			return true;
		}

		this.shippingPrice = null;
		return false;
	}

	@Override
	public String toString() {
		return null;
	}
}