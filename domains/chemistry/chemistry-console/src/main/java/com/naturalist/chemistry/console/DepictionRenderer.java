package com.naturalist.chemistry.console;

import com.naturalist.chemistry.compound.CompoundDepiction;
import com.naturalist.chemistry.compound.DepictionId;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
class DepictionRenderer {

    private final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
    private final DepictionGenerator depictionGenerator = new DepictionGenerator()
            .withSize(900, 700)
            .withAtomColors()
            .withZoom(2.5)
            .withFillToFit();
    private final Map<DepictionId, String> svgCache = new ConcurrentHashMap<>();

    String renderSvg(CompoundDepiction depiction) {
        return svgCache.computeIfAbsent(depiction.name(), key -> render(depiction.smiles()));
    }

    private String render(String smiles) {
        try {
            IAtomContainer molecule = smilesParser.parseSmiles(smiles);
            return depictionGenerator.depict(molecule).toSvgStr();
        } catch (CDKException e) {
            throw new IllegalStateException("Failed to render SMILES: " + smiles, e);
        }
    }
}
