/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.price.spot;

import bisq.price.spot.providers.BitcoinAverage;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * High-level {@link ExchangeRate} data operations.
 */
@Service
class ExchangeRateService {

    private final List<ExchangeRateProvider> providers;

    /**
     * Construct an {@link ExchangeRateService} with a list of all
     * {@link ExchangeRateProvider} implementations discovered via classpath scanning.
     *
     * @param providers all {@link ExchangeRateProvider} implementations in ascending
     *                  order of precedence
     */
    public ExchangeRateService(List<ExchangeRateProvider> providers) {
        this.providers = providers;
    }

    public Map<String, Object> getAllMarketPrices() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        Map<String, ExchangeRate> allExchangeRates = new LinkedHashMap<>();

        providers.forEach(p -> {
            Set<ExchangeRate> exchangeRates = p.get();
            metadata.putAll(getMetadata(p, exchangeRates));
            exchangeRates.forEach(e ->
                allExchangeRates.put(e.getCurrency(), e)
            );
        });

        return new LinkedHashMap<String, Object>() {{
            putAll(metadata);
            put("data", allExchangeRates.values());
        }};
    }

    private Map<String, Object> getMetadata(ExchangeRateProvider provider, Set<ExchangeRate> exchangeRates) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        long timestamp = getTimestamp(provider, exchangeRates);

        if (provider instanceof BitcoinAverage.Local) {
            metadata.put("btcAverageTs", timestamp);
        }

        String prefix = provider.getPrefix();
        metadata.put(prefix + "Ts", timestamp);
        metadata.put(prefix + "Count", exchangeRates.size());

        return metadata;
    }

    private long getTimestamp(ExchangeRateProvider provider, Set<ExchangeRate> exchangeRates) {
        return exchangeRates.stream()
            .filter(e -> provider.getName().equals(e.getProvider()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No exchange rate data found for " + provider.getName()))
            .getTimestamp();
    }
}
