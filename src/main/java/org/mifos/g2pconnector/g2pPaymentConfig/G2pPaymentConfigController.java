package org.mifos.g2pconnector.g2pPaymentConfig;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/g2pPaymentConfig")
public class G2pPaymentConfigController {
    @Autowired
    private G2pPaymentConfigService g2PPaymentConfigService;

    @GetMapping
    public List<G2pPaymentConfig> getAllG2PProgramConfigs() {
        return g2PPaymentConfigService.getAllG2PProgramConfigs();
    }

    @GetMapping("/{id}")
    public G2pPaymentConfig getG2PProgramConfigById(@PathVariable Long id) {
        return g2PPaymentConfigService.getG2PProgramConfigById(id);
    }

    @PostMapping
    public G2pPaymentConfig createG2PProgramConfig(@RequestBody G2pPaymentConfig g2PPaymentConfig) {
        return g2PPaymentConfigService.saveG2PProgramConfig(g2PPaymentConfig);
    }

    @PutMapping("/{id}")
    public G2pPaymentConfig updateG2PProgramConfig(@PathVariable Long id, @RequestBody G2pPaymentConfig g2PPaymentConfig) {
        g2PPaymentConfig.setConfigId(id);
        return g2PPaymentConfigService.saveG2PProgramConfig(g2PPaymentConfig);
    }

    @DeleteMapping("/{id}")
    public void deleteG2PProgramConfig(@PathVariable Long id) {
        g2PPaymentConfigService.deleteG2PProgramConfig(id);
    }
}
