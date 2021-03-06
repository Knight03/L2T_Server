/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package l2server.gameserver.util;

import java.util.*;

/**
 * Descending Integer Sort Algorithm
 * <p>
 * - Fast ordering system.
 * - Can easily be ported elsewhere.
 * - Can handle any number of values, from a list or even from a map.
 * - Handles duplicate values.
 *
 * @author Tempy
 */
public class MultiSort {
	public static final int SORT_ASCENDING = 0;
	public static final int SORT_DESCENDING = 1;

	private List<?> keyList;
	private List<Integer> valueList;

	private boolean isSortDescending;
	private boolean isSorted;

	public MultiSort(int[] valueList) {
		this.valueList = getIntList(valueList);
	}

	public MultiSort(Collection<Integer> valueList) {
		this.valueList = getIntList(valueList);
	}

	public MultiSort(Object[] keyList, int[] valueList) {
		this.keyList = getList(keyList);
		this.valueList = getIntList(valueList);
	}

	public MultiSort(Map<?, Integer> valueMap) {
		keyList = getList(valueMap.keySet());
		valueList = getIntList(valueMap.values());
	}

	private List<Integer> getIntList(Collection<Integer> valueList) {
		return Arrays.asList(valueList.toArray(new Integer[valueList.size()]));
	}

	private List<Integer> getIntList(int[] valueList) {
		Integer[] tempIntList = new Integer[valueList.length];

		for (int i = 0; i < valueList.length; i++) {
			tempIntList[i] = valueList[i];
		}

		return Arrays.asList(tempIntList);
	}

	private List<?> getList(Collection<?> valueList) {
		return getList(valueList.toArray(new Object[valueList.size()]));
	}

	private List<Object> getList(Object[] valueList) {
		return Arrays.asList(valueList);
	}

	public final int getCount() {
		return getValues().size();
	}

	public final int getHarmonicMean() {
		if (getValues().isEmpty()) {
			return -1;
		}

		int totalValue = 0;

		for (int currValue : getValues()) {
			totalValue += 1 / currValue;
		}

		return getCount() / totalValue;
	}

	public final List<?> getKeys() {
		if (keyList == null) {
			return new ArrayList<>();
		}

		return keyList;
	}

	public final int getFrequency(int checkValue) {
		return Collections.frequency(getValues(), checkValue);
	}

	public final int getMaxValue() {
		return Collections.max(getValues());
	}

	public final int getMinValue() {
		return Collections.min(getValues());
	}

	public final int getMean() {
		if (getValues().isEmpty()) {
			return -1;
		}

		return getTotalValue() / getCount();
	}

	public final double getStandardDeviation() {
		if (getValues().isEmpty()) {
			return -1;
		}

		List<Double> tempValList = new ArrayList<>();

		int meanValue = getMean();
		int numValues = getCount();

		for (int value : getValues()) {
			double adjValue = Math.pow(value - meanValue, 2);
			tempValList.add(adjValue);
		}

		double totalValue = 0;

		for (double storedVal : tempValList) {
			totalValue += storedVal;
		}

		return Math.sqrt(totalValue / (numValues - 1));
	}

	public final int getTotalValue() {
		if (getValues().isEmpty()) {
			return 0;
		}

		int totalValue = 0;

		for (int currValue : getValues()) {
			totalValue += currValue;
		}

		return totalValue;
	}

	public final List<Integer> getValues() {
		if (valueList == null) {
			return new ArrayList<>();
		}

		return valueList;
	}

	public final boolean isSortDescending() {
		return isSortDescending;
	}

	public final boolean isSorted() {
		return isSorted;
	}

	public final void setSortDescending(boolean isDescending) {
		isSortDescending = isDescending;
	}

	public boolean sort() {
		try {
			List<Object> newKeyList = new ArrayList<>();
			List<Integer> newValueList = new ArrayList<>();

			// Sort the list of values in ascending numerical order.
			Collections.sort(getValues());

			int lastValue = 0;

			if (!isSortDescending()) {
				// If there are no keys, just return the ascendingly sorted values.
				if (getKeys().isEmpty()) {
					return true;
				}

				// Iterate through the list of ordered numerical values.
				for (int i = getValues().size() - 1; i > -1; i--) {
					int currValue = getValues().get(i);

					// If the current value is equal to the last value, we have at least one
					// duplicate that has been outputted already, so continue.
					if (currValue == lastValue) {
						continue;
					}

					// Set the last value to the current value, to prevent duplication.
					lastValue = currValue;

					// Iterate through each key and match it to its stored integer value,
					// then output both sets of data in the correct descending numerical order.
					for (int j = 0; j < getKeys().size(); j++) {
						Object currKey = getKeys().get(j);

						if (getValues().get(j) == currValue) {
							newKeyList.add(currKey);
							newValueList.add(currValue);
						}
					}
				}
			} else {
				// If there are no keys, just sort the value list in reverse order.
				if (getKeys().isEmpty()) {
					Collections.reverse(getValues());
					return true;
				}

				// Do the exact same as above, but in descending order.
				for (int i = 0; i < getValues().size(); i++) {
					int currValue = getValues().get(i);

					if (currValue == lastValue) {
						continue;
					}

					lastValue = currValue;

					for (int j = 0; j < getKeys().size(); j++) {
						Object currKey = getKeys().get(j);

						if (getValues().get(j) == currValue) {
							newKeyList.add(currKey);
							newValueList.add(currValue);
						}
					}
				}
			}

			keyList = newKeyList;
			valueList = newValueList;
			isSorted = true;
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
