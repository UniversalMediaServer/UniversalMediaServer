import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ToolsKitchenOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-tools-kitchen-off",
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
    d: "M7 3h5l-.5 4.5m-.4 3.595l-.1 .905h-6l-.875 -7.874"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 18h2v3h-2z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15.225 11.216c.42 -2.518 1.589 -5.177 4.775 -8.216v12h-1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 15v1m0 4v1h-1v-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 12v6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { ToolsKitchenOff as default };
