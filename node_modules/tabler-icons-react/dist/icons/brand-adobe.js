import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BrandAdobe(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-brand-adobe",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12.893 4.514l7.977 13.994a0.993 .993 0 0 1 -.394 1.365a1.04 1.04 0 0 1 -.5 .127h-3.476l-4.5 -8l-2.5 4h1.5l2 4h-8.977c-.565 0 -1.023 -.45 -1.023 -1.003c0 -.171 .045 -.34 .13 -.49l7.977 -13.993a1.034 1.034 0 0 1 1.786 0z"
  }));
}

export { BrandAdobe as default };
